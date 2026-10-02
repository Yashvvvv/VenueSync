package com.venuesync.app.core.repository

import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.Ticket
import com.venuesync.app.core.model.TicketFilter
import com.venuesync.app.core.model.TicketSummary
import com.venuesync.app.core.model.toDomainOrNull
import com.venuesync.app.core.network.TicketsApi
import javax.inject.Inject

/** A page of the user's tickets plus whether the server has more. */
data class TicketPage(val tickets: List<TicketSummary>, val isLast: Boolean)

interface TicketsRepository {
    /** Buys one ticket. Repeating with the same [idempotencyKey] returns the same ticket, never a second one. */
    suspend fun purchase(eventId: String, ticketTypeId: String, idempotencyKey: String): Result<Ticket>
    suspend fun listTickets(filter: TicketFilter, page: Int = 0): Result<TicketPage>
    suspend fun getTicket(id: String): Result<Ticket>
    /** The ticket's QR code as PNG bytes, checked to really be a PNG of sane size. */
    suspend fun getQrCode(ticketId: String): Result<ByteArray>
}

/** Trust boundary for tickets: same rules as [EventsRepositoryImpl]. */
class TicketsRepositoryImpl @Inject constructor(
    private val api: TicketsApi,
) : TicketsRepository {

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
        }
    }

    override suspend fun listTickets(filter: TicketFilter, page: Int): Result<TicketPage> = apiCall {
        val response = api.listTickets(filter.wire, page)
        // One broken row must not hide the others; duplicate ids would crash LazyColumn keys.
        TicketPage(response.content.mapNotNull { it.toDomainOrNull() }.distinctBy { it.id }, isLast = response.last)
    }

    override suspend fun getTicket(id: String): Result<Ticket> {
        if (!UuidRegex.matches(id)) return Result.failure(ApiException(ApiError.NotFound))
        return apiCall {
            api.getTicket(id).toDomainOrNull()
                ?.takeIf { it.id.equals(id, ignoreCase = true) } // server answered the question we asked
                ?: throw ApiException(ApiError.InvalidResponse)
        }
    }

    override suspend fun getQrCode(ticketId: String): Result<ByteArray> {
        if (!UuidRegex.matches(ticketId)) return Result.failure(ApiException(ApiError.NotFound))
        return apiCall {
            api.getQrCode(ticketId).takeIf { it.size <= MAX_QR_BYTES && it.startsWith(PngSignature) }
                ?: throw ApiException(ApiError.InvalidResponse)
        }
    }

    private fun ByteArray.startsWith(prefix: ByteArray) =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

    private companion object {
        /** The real image is ~1 KB (300x300, two colours); anything near this cap is not our QR code. */
        const val MAX_QR_BYTES = 1_000_000
        val PngSignature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    }
}
