package com.venuesync.app.core.repository

import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.TicketDto
import com.venuesync.app.core.model.TicketFilter
import com.venuesync.app.core.network.TicketsApi
import com.venuesync.app.core.network.VenueSyncHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import java.time.Instant
import java.util.Base64
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tickets on the phone for venues with no signal: what's saved, when it's used, when it's dropped. */
class OfflineTicketsTest {

    private val a = "aaaaaaaa-0000-4000-8000-000000000001"
    private val b = "bbbbbbbb-0000-4000-8000-000000000002"
    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 7)
    private val nowAt = Instant.parse("2026-10-04T06:00:00Z") // 11:30 in India

    private val cache = FakeTicketCache()
    private var offline = false
    private val routes = mutableMapOf<String, Pair<HttpStatusCode, ByteArray>>()
    private val requested = mutableListOf<String>()

    private val repo = TicketsRepositoryImpl(
        TicketsApi(
            VenueSyncHttpClient.create(
                "https://example.test/api/v1",
                enableLogging = false,
                engine = MockEngine { request ->
                    val key = request.url.encodedPath.removePrefix("/api/v1/") +
                        (request.url.parameters["page"]?.let { "?page=$it" } ?: "")
                    requested += key
                    if (offline) throw IOException("no signal")
                    val (status, body) = routes[key] ?: (HttpStatusCode.NotFound to "{}".toByteArray())
                    val type = if (key.endsWith("qr-codes")) "image/png" else "application/json"
                    respond(body, status, headersOf(HttpHeaders.ContentType, type))
                },
                retryBaseDelayMs = 0,
            ),
        ),
        cache,
        NoBackgroundWork,
    ).apply { now = { nowAt } }

    private fun ticketJson(id: String, status: String = "PURCHASED", start: String = "2026-10-05T19:00:00+05:30", end: String = "2026-10-05T23:00:00+05:30") =
        """{"id":"$id","ticketCode":"AAAA-0001","status":"$status","ticketTypeName":"GA","eventId":"e","eventName":"Show $id","eventStart":"$start","eventEnd":"$end"}"""

    private fun serve(path: String, body: String, status: HttpStatusCode = HttpStatusCode.OK) {
        routes[path] = status to body.toByteArray()
    }

    private fun serveList(page: Int, last: Boolean, vararg ids: String) = serve(
        "tickets?page=$page",
        """{"content":[${ids.joinToString { """{"id":"$it","status":"PURCHASED","ticketType":{"name":"GA"},"eventName":"Show"}""" }}],"last":$last}""",
    )

    private fun saved(id: String, qr: Boolean = true, end: String = "2026-10-05T23:00:00+05:30") = CachedTicket(
        ticket = TicketDto(id = id, ticketCode = "AAAA-0001", status = "PURCHASED", ticketTypeName = "GA", eventId = "e",
            eventName = "Show $id", eventStart = "2026-10-05T19:00:00+05:30", eventEnd = end),
        qrPng = if (qr) Base64.getEncoder().encodeToString(png) else null,
        savedAt = nowAt.minusSeconds(3600).toEpochMilli(),
    )

    @Test
    fun `a live ticket and its code are saved`() = runTest {
        serve("tickets/$a", ticketJson(a))
        routes["tickets/$a/qr-codes"] = HttpStatusCode.OK to png

        assertNull(repo.getTicket(a).getOrThrow().savedAt) // live data says so
        repo.getQrCode(a).getOrThrow()

        val entry = cache.saved.single()
        assertEquals(a, entry.ticket.id)
        assertArrayEquals(png, Base64.getDecoder().decode(entry.qrPng))
    }

    @Test
    fun `no signal answers from the phone, and says how old the copy is`() = runTest {
        cache.saved = listOf(saved(a))
        offline = true

        val ticket = repo.getTicket(a).getOrThrow()
        assertEquals("AAAA-0001", ticket.code)
        assertEquals(nowAt.minusSeconds(3600), ticket.savedAt)
        assertArrayEquals(png, repo.getQrCode(a).getOrThrow())
    }

    @Test
    fun `a server error falls back too, but a real answer never does`() = runTest {
        cache.saved = listOf(saved(a))
        serve("tickets/$a", "{}", HttpStatusCode.ServiceUnavailable)
        assertNotNull(repo.getTicket(a).getOrThrow().savedAt) // Render cold start at the door

        serve("tickets/$a", "{}", HttpStatusCode.NotFound)
        assertEquals(ApiError.NotFound, (repo.getTicket(a).exceptionOrNull() as ApiException).error)
    }

    @Test
    fun `nothing saved means the network error stands`() = runTest {
        offline = true
        assertEquals(ApiError.Network, (repo.getTicket(a).exceptionOrNull() as ApiException).error)
        assertEquals(ApiError.Network, (repo.listTickets(TicketFilter.Active).exceptionOrNull() as ApiException).error)
    }

    @Test
    fun `a ticket that was used is dropped from the phone`() = runTest {
        cache.saved = listOf(saved(a))
        serve("tickets/$a", ticketJson(a, status = "USED"))

        repo.getTicket(a).getOrThrow()
        assertTrue(cache.saved.isEmpty())
    }

    @Test
    fun `offline list is the upcoming saved tickets, soonest first, Active only`() = runTest {
        val ended = saved(b, end = "2026-10-04T10:00:00+05:30") // ended 90 minutes ago
        cache.saved = listOf(saved(a), ended)
        offline = true

        val page = repo.listTickets(TicketFilter.Active).getOrThrow()
        assertEquals(listOf(a), page.tickets.map { it.id })
        assertTrue(page.isLast)
        assertNotNull(page.savedAt)
        assertTrue(repo.listTickets(TicketFilter.Past).isFailure)
    }

    @Test
    fun `sync saves every active ticket with its code and drops the rest`() = runTest {
        cache.saved = listOf(saved(b)) // b was used since: it's no longer in the Active list
        serveList(0, last = false, a)
        serveList(1, last = true)
        serve("tickets/$a", ticketJson(a))
        routes["tickets/$a/qr-codes"] = HttpStatusCode.OK to png

        repo.syncNow(force = false)

        assertEquals(listOf(a), cache.saved.map { it.ticket.id })
        assertNotNull(cache.saved.single().qrPng)
    }

    @Test
    fun `a sync that couldn't see the whole list drops nothing`() = runTest {
        cache.saved = listOf(saved(b))
        serveList(0, last = false, a)
        serve("tickets?page=1", "{}", HttpStatusCode.ServiceUnavailable)

        repo.syncNow(force = false)

        assertEquals(listOf(b), cache.saved.map { it.ticket.id })
    }

    @Test
    fun `a saved ticket with its code isn't fetched again, and a recent sync is skipped`() = runTest {
        cache.saved = listOf(saved(a))
        serveList(0, last = true, a)

        repo.syncNow(force = false)
        assertEquals(listOf("tickets?page=0"), requested)

        repo.syncNow(force = false) // under a minute later
        assertEquals(1, requested.size)
        repo.syncNow(force = true) // after a purchase
        assertEquals(2, requested.size)
    }
}
