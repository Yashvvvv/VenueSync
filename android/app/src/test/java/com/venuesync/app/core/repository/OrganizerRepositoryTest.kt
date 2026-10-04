package com.venuesync.app.core.repository

import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.EventDraft
import com.venuesync.app.core.model.EventStatus
import com.venuesync.app.core.model.Refusal
import com.venuesync.app.core.model.TicketTypeDraft
import com.venuesync.app.core.network.IDEMPOTENCY_KEY_HEADER
import com.venuesync.app.core.network.OrganizerApi
import com.venuesync.app.core.network.VenueSyncHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Drives the real client config (retry, JSON, error codes) against a fake server. */
class OrganizerRepositoryTest {

    private val eventId = "3f2b8c1e-9a4d-4e6f-8b7a-1c2d3e4f5a6b"
    private val typeId = "7c1e2d3f-4a5b-4c6d-8e9f-0a1b2c3d4e5f"
    private val key = "9e8d7c6b-5a4f-4e3d-a2c1-b0a9f8e7d6c5"
    private val requests = mutableListOf<HttpRequestData>()
    private val bodies = mutableListOf<String>()

    private fun repo(status: HttpStatusCode = HttpStatusCode.OK, body: String = "{}") = OrganizerRepositoryImpl(
        OrganizerApi(
            VenueSyncHttpClient.create(
                "https://example.test/api/v1",
                enableLogging = false,
                engine = MockEngine { request ->
                    requests += request
                    bodies += String(request.body.toByteArray())
                    respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
                },
                retryBaseDelayMs = 0,
            ),
        ),
    )

    private fun Result<*>.error() = (exceptionOrNull() as ApiException).error

    private val eventJson = """{"id":"$eventId","name":"Show","venue":"Hall","status":"PUBLISHED",
        "start":"2026-11-20T19:00:00+05:30","end":"2026-11-20T23:00:00+05:30",
        "ticketTypes":[{"id":"$typeId","name":"GA","price":25.5,"totalAvailable":100,"sold":40}]}"""

    private val draft = EventDraft(
        name = "Show",
        venue = "Hall",
        start = LocalDateTime.of(2026, 11, 20, 19, 0),
        end = LocalDateTime.of(2026, 11, 20, 23, 0),
        status = EventStatus.Draft,
        ticketTypes = listOf(TicketTypeDraft(name = "GA", price = BigDecimal("25.50"), capacity = 100)),
    )

    @Test
    fun `an event maps with sales per ticket type and keeps the wall clock`() = runTest {
        val event = repo(body = eventJson).event(eventId).getOrThrow()
        assertEquals("/api/v1/events/$eventId", requests.single().url.encodedPath)
        assertEquals(EventStatus.Published, event.status)
        assertEquals(LocalDateTime.of(2026, 11, 20, 19, 0), event.start)
        assertEquals(40L, event.ticketTypes.single().sold)
        assertEquals(100, event.ticketTypes.single().capacity)
        assertEquals(BigDecimal("1020.0"), event.grossAtCurrentPrices)
    }

    @Test
    fun `a ticket type without an id fails the whole event, so an edit can never drop it`() = runTest {
        val broken = """{"id":"$eventId","name":"Show","venue":"Hall","status":"DRAFT","ticketTypes":[{"name":"GA","price":1}]}"""
        assertEquals(ApiError.InvalidResponse, repo(body = broken).event(eventId).error())
    }

    @Test
    fun `an answer about another event is rejected`() = runTest {
        val other = eventJson.replace(eventId, "00000000-0000-0000-0000-000000000000")
        assertEquals(ApiError.InvalidResponse, repo(body = other).event(eventId).error())
    }

    @Test
    fun `create sends the key, wall-clock dates without an offset, and prices as numbers`() = runTest {
        repo(body = eventJson).create(draft, key).getOrThrow()
        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals(key, request.headers[IDEMPOTENCY_KEY_HEADER])
        val sent = bodies.single()
        assertTrue(sent, sent.contains("\"start\":\"2026-11-20T19:00:00\""))
        assertTrue(sent, sent.contains("\"status\":\"DRAFT\""))
        assertTrue(sent, sent.contains("\"price\":25.5"))
        assertTrue(sent, !sent.contains("\"id\":")) // a create carries no ids
    }

    @Test
    fun `an invalid draft is marked on the phone and never sent`() = runTest {
        val repo = repo()
        assertEquals(ApiError.Invalid("name"), repo.create(draft.copy(name = " "), key).error())
        assertEquals(ApiError.Invalid("end"), repo.create(draft.copy(end = draft.start!!.minusHours(1)), key).error())
        assertEquals(ApiError.Invalid("ticketTypes"), repo.create(draft.copy(ticketTypes = emptyList()), key).error())
        val oversold = draft.copy(ticketTypes = listOf(TicketTypeDraft(typeId, "GA", BigDecimal.ONE, capacity = 5, sold = 8)))
        assertEquals(ApiError.Invalid("ticketTypes[0].capacity"), repo.update(eventId, oversold).error())
        assertEquals(ApiError.Invalid("status"), repo.create(draft.copy(status = EventStatus.Cancelled), key).error())
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `update sends the event id and existing ticket type ids`() = runTest {
        val edit = draft.copy(ticketTypes = listOf(TicketTypeDraft(typeId, "GA", BigDecimal.TEN, capacity = 100, sold = 40)))
        repo(body = eventJson).update(eventId, edit).getOrThrow()
        assertEquals(HttpMethod.Put, requests.single().method)
        assertTrue(bodies.single().contains("\"id\":\"$eventId\""))
        assertTrue(bodies.single().contains("\"id\":\"$typeId\""))
    }

    @Test
    fun `refusals and field errors keep their meaning`() = runTest {
        fun body(code: String, field: String? = null) =
            """{"code":"$code","error":"server text"${field?.let { ",\"field\":\"$it\"" } ?: ""}}"""
        assertEquals(
            ApiError.Refused(Refusal.TicketTypeHasSales),
            repo(HttpStatusCode.Conflict, body("TICKET_TYPE_HAS_SALES")).update(eventId, draft).error(),
        )
        assertEquals(
            ApiError.Refused(Refusal.EventHasSales),
            repo(HttpStatusCode.Conflict, body("EVENT_HAS_SALES")).delete(eventId).error(),
        )
        assertEquals(
            ApiError.Refused(Refusal.CapacityBelowSold),
            repo(HttpStatusCode.Conflict, body("CAPACITY_BELOW_SOLD")).update(eventId, draft).error(),
        )
        assertEquals(
            ApiError.Refused(Refusal.StatusChange),
            repo(HttpStatusCode.Conflict, body("STATUS_CHANGE_INVALID")).update(eventId, draft).error(),
        )
        assertEquals(
            ApiError.Invalid("salesEnd"),
            repo(HttpStatusCode.BadRequest, body("EVENT_INVALID", "salesEnd")).update(eventId, draft).error(),
        )
        assertEquals(ApiError.Conflict, repo(HttpStatusCode.Conflict, body("SOMETHING_NEW")).update(eventId, draft).error())
    }

    @Test
    fun `a list drops one malformed event and keeps the rest`() = runTest {
        val page = """{"content":[$eventJson,{"id":"","name":"x"}],"last":true}"""
        val result = repo(body = page).events(EventStatus.Published, page = 0).getOrThrow()
        assertEquals(listOf(eventId), result.events.map { it.id })
        assertEquals("PUBLISHED", requests.single().url.parameters["status"])
    }

    @Test
    fun `counts default missing statuses to zero`() = runTest {
        val counts = repo(body = """{"draft":2,"published":5}""").counts().getOrThrow()
        assertEquals(2L, counts.draft)
        assertEquals(5L, counts.published)
        assertEquals(0L, counts.completed)
    }

    @Test
    fun `malformed ids never reach a URL`() = runTest {
        val repo = repo()
        assertEquals(ApiError.NotFound, repo.event("../admin").error())
        assertEquals(ApiError.NotFound, repo.delete("1-1-1-1-1").error())
        assertEquals(ApiError.NotFound, repo.removeStaff(eventId, "nope").error())
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `an invite without a code is an invalid response`() = runTest {
        assertEquals(ApiError.InvalidResponse, repo(body = """{"expiresAt":"2026-11-27T19:00:00"}""").createInvite(eventId).error())
        val invite = repo(body = """{"code":"K7Q2M-9XH4P","expiresAt":"2026-11-27T19:00:00+05:30"}""").createInvite(eventId).getOrThrow()
        assertEquals("K7Q2M-9XH4P", invite.code)
    }
}
