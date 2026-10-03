package com.venuesync.app.core.repository

import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.Ticket
import com.venuesync.app.core.model.TicketDto
import com.venuesync.app.core.model.TicketFilter
import com.venuesync.app.core.model.TicketStatus
import com.venuesync.app.core.model.TicketSummary
import com.venuesync.app.core.model.toDomainOrNull
import com.venuesync.app.core.network.TicketsApi
import com.venuesync.app.di.ApplicationScope
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Base64
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A page of the user's tickets plus whether the server has more. [savedAt] is set when it came from the phone. */
data class TicketPage(val tickets: List<TicketSummary>, val isLast: Boolean, val savedAt: Instant? = null)

interface TicketsRepository {
    /** Buys one ticket. Repeating with the same [idempotencyKey] returns the same ticket, never a second one. */
    suspend fun purchase(eventId: String, ticketTypeId: String, idempotencyKey: String): Result<Ticket>
    /** Active page 0 falls back to the tickets saved on the phone when there's no signal. */
    suspend fun listTickets(filter: TicketFilter, page: Int = 0): Result<TicketPage>
    /** Falls back to the saved copy when there's no signal; [Ticket.savedAt] says so. */
    suspend fun getTicket(id: String): Result<Ticket>
    /** The ticket's QR code as PNG bytes, checked to really be a PNG of sane size. Falls back like [getTicket]. */
    suspend fun getQrCode(ticketId: String): Result<ByteArray>

    /** The QR code saved on the phone, without trying the network. It never changes, so this is always safe to show. */
    suspend fun savedQrCode(ticketId: String): ByteArray?

    /**
     * Saves the upcoming tickets (details + QR code) on the phone so they open with no signal. Fire and forget;
     * one at a time; skipped if one finished under a minute ago, unless [force] (a purchase just happened).
     */
    fun syncOffline(force: Boolean = false)
}

/** Trust boundary for tickets: same rules as [EventsRepositoryImpl]. */
class TicketsRepositoryImpl @Inject constructor(
    private val api: TicketsApi,
    private val cache: TicketCache,
    @ApplicationScope private val scope: CoroutineScope,
) : TicketsRepository {

    /** Overridable so tests control "now". */
    internal var now: () -> Instant = Instant::now

    private val cacheLock = Mutex() // the saved set is read-modify-written as a whole
    private val syncLock = Mutex()
    private var lastSync: Instant? = null

    override suspend fun purchase(eventId: String, ticketTypeId: String, idempotencyKey: String): Result<Ticket> {
        // Ids land in the URL path; anything that isn't a UUID never reaches the network.
        if (!UuidRegex.matches(eventId) || !UuidRegex.matches(ticketTypeId)) {
            return Result.failure(ApiException(ApiError.NotFound))
        }
        // The server would answer 400; failing here keeps a client bug from costing a round trip.
        if (!UuidRegex.matches(idempotencyKey)) {
            return Result.failure(ApiException(ApiError.Unknown("invalid idempotency key")))
        }
        return apiCall {
            api.purchase(eventId, ticketTypeId, idempotencyKey).toDomainOrNull()
                ?.takeIf { UuidRegex.matches(it.id) && it.eventId.equals(eventId, ignoreCase = true) }
                ?: throw ApiException(ApiError.InvalidResponse)
        }.onSuccess { syncOffline(force = true) } // a ticket bought at home must open at the door with no signal
    }

    override suspend fun listTickets(filter: TicketFilter, page: Int): Result<TicketPage> = apiCall {
        val response = api.listTickets(filter.wire, page)
        // One broken row must not hide the others; duplicate ids would crash LazyColumn keys.
        TicketPage(response.content.mapNotNull { it.toDomainOrNull() }.distinctBy { it.id }, isLast = response.last)
    }.recoverOffline {
        if (filter != TicketFilter.Active || page != 0) return@recoverOffline null
        val saved = upcoming().takeIf { it.isNotEmpty() } ?: return@recoverOffline null // "No connection", not "no tickets"
        TicketPage(
            tickets = saved.sortedBy { it.eventStart }
                .map { TicketSummary(it.id, it.status, it.ticketTypeName, it.eventName, it.eventStart) },
            isLast = true,
            savedAt = saved.minOf { it.savedAt!! }, // the oldest copy sets the honest age
        )
    }

    override suspend fun getTicket(id: String): Result<Ticket> {
        if (!UuidRegex.matches(id)) return Result.failure(ApiException(ApiError.NotFound))
        return apiCall {
            val dto = api.getTicket(id)
            val ticket = dto.toDomainOrNull()
                ?.takeIf { it.id.equals(id, ignoreCase = true) } // server answered the question we asked
                ?: throw ApiException(ApiError.InvalidResponse)
            dto to ticket
        }.onSuccess { (dto, ticket) ->
            // Only a ticket that can still get in is worth carrying offline.
            if (ticket.hasCode()) save(dto) else forget(id)
        }.map { it.second }
            .recoverOffline { saved(id)?.toTicket() }
    }

    override suspend fun getQrCode(ticketId: String): Result<ByteArray> {
        if (!UuidRegex.matches(ticketId)) return Result.failure(ApiException(ApiError.NotFound))
        return apiCall {
            api.getQrCode(ticketId).takeIf { it.size <= MAX_QR_BYTES && it.startsWith(PngSignature) }
                ?: throw ApiException(ApiError.InvalidResponse)
        }.onSuccess { attachQr(ticketId, it) }
            .recoverOffline { savedQrCode(ticketId) }
    }

    override suspend fun savedQrCode(ticketId: String): ByteArray? =
        saved(ticketId)?.qrPng?.let { runCatching { Base64.getDecoder().decode(it) }.getOrNull() }

    override fun syncOffline(force: Boolean) {
        scope.launch { syncNow(force) }
    }

    internal suspend fun syncNow(force: Boolean) = syncLock.withLock {
        val last = lastSync
        if (!force && last != null && Duration.between(last, now()) < MIN_SYNC_GAP) return@withLock
        if (sync()) lastSync = now()
    }

    /** True when every Active ticket is saved. Any failure just stops: the next sync tries again. */
    private suspend fun sync(): Boolean {
        val listed = mutableListOf<TicketSummary>()
        var complete = false
        for (page in 0 until MAX_SYNC_PAGES) {
            val next = apiCall { api.listTickets(TicketFilter.Active.wire, page) }.getOrNull() ?: return false
            listed += next.content.mapNotNull { it.toDomainOrNull() }
            if (next.last) {
                complete = true
                break
            }
        }
        val ids = listed.filter { it.status == TicketStatus.Purchased || it.status == TicketStatus.Unknown }
            .map { it.id }.distinct().take(MAX_SAVED)
        // Used, cancelled or ended tickets leave the Active list; only the complete list proves one is gone.
        if (complete) edit { saved -> saved.filter { it.ticket.id in ids } }
        // The server just listed these as still valid: the copy is confirmed as of now, which is what the banner's age means.
        val confirmedAt = now().toEpochMilli()
        edit { saved -> saved.map { if (it.ticket.id in ids) it.copy(savedAt = confirmedAt) else it } }
        // getTicket/getQrCode save as they go. Their offline fallback can't fire here: there's nothing saved to fall back to.
        for (id in ids) {
            if (saved(id) == null && getTicket(id).isFailure) return false
            val entry = saved(id) ?: continue // not usable after all (the detail said used or cancelled)
            if (entry.qrPng == null && getQrCode(id).isFailure) return false
        }
        return complete
    }

    /** On Network or 5xx (no signal, server cold start), answer from the phone. Any other answer is real. */
    private suspend fun <T> Result<T>.recoverOffline(fallback: suspend () -> T?): Result<T> {
        val error = (exceptionOrNull() as? ApiException)?.error ?: return this
        if (error != ApiError.Network && error !is ApiError.Server) return this
        return fallback()?.let { Result.success(it) } ?: this
    }

    private suspend fun saved(id: String): CachedTicket? =
        readSaved().firstOrNull { it.ticket.id.equals(id, ignoreCase = true) }

    /** Saved tickets whose event hasn't ended. Times are India wall clock, as the API sends them (+05:30). */
    private suspend fun upcoming(): List<Ticket> = readSaved().mapNotNull { it.toTicket() }.filter { ticket ->
        val end = ticket.eventEnd ?: ticket.eventStart?.plusDays(1) ?: return@filter true
        end.atZone(EventZone).toInstant().isAfter(now())
    }

    private suspend fun save(dto: TicketDto) = edit { saved ->
        val previous = saved.firstOrNull { it.ticket.id == dto.id }
        val entry = CachedTicket(dto, qrPng = previous?.qrPng, savedAt = now().toEpochMilli())
        (saved.filter { it.ticket.id != dto.id } + entry).sortedByDescending { it.savedAt }.take(MAX_SAVED)
    }

    private suspend fun forget(id: String) = edit { saved -> saved.filter { !it.ticket.id.equals(id, ignoreCase = true) } }

    private suspend fun attachQr(id: String, png: ByteArray) = edit { saved ->
        val encoded = Base64.getEncoder().encodeToString(png)
        saved.map { if (it.ticket.id.equals(id, ignoreCase = true)) it.copy(qrPng = encoded) else it }
    }

    /** The phone's copy is a convenience: a storage failure never fails a live answer. */
    private suspend fun readSaved(): List<CachedTicket> = guard(emptyList()) { cache.read() }

    private suspend fun edit(change: (List<CachedTicket>) -> List<CachedTicket>) = guard(Unit) {
        cacheLock.withLock {
            val before = cache.read()
            val after = change(before)
            if (after != before) cache.write(after)
        }
    }

    private inline fun <T> guard(fallback: T, block: () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        fallback
    }

    private fun CachedTicket.toTicket(): Ticket? = ticket.toDomainOrNull()?.copy(savedAt = Instant.ofEpochMilli(savedAt))

    private fun Ticket.hasCode() = status == TicketStatus.Purchased || status == TicketStatus.Unknown

    private fun ByteArray.startsWith(prefix: ByteArray) =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

    private companion object {
        /** The real image is ~1 KB (300x300, two colours); anything near this cap is not our QR code. */
        const val MAX_QR_BYTES = 1_000_000
        const val MAX_SYNC_PAGES = 5
        const val MAX_SAVED = 50
        val MIN_SYNC_GAP: Duration = Duration.ofMinutes(1)
        val EventZone: ZoneId = ZoneId.of("Asia/Kolkata")
        val PngSignature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    }
}
