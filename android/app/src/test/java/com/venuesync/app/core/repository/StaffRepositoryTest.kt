package com.venuesync.app.core.repository

import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.JoinedEvent
import com.venuesync.app.core.model.TicketStatus
import com.venuesync.app.core.network.StaffApi
import com.venuesync.app.core.network.TicketsApi
import com.venuesync.app.core.network.VenueSyncHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Door staff calls through the real client config against a fake server. */
class StaffRepositoryTest {

    private val eventId = "3f2b8c1e-9a4d-4e6f-8b7a-1c2d3e4f5a6b"
    private val key = "9e8d7c6b-5a4f-4e3d-a2c1-b0a9f8e7d6c5"
    private val requests = mutableListOf<HttpRequestData>()
    private val bodies = mutableListOf<String>()

    private fun client(status: HttpStatusCode, body: String): HttpClient = VenueSyncHttpClient.create(
        "https://example.test/api/v1",
        enableLogging = false,
        engine = MockEngine { request ->
            requests += request
            bodies += String(request.body.toByteArray())
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        },
        retryBaseDelayMs = 0,
    )

    private fun staff(status: HttpStatusCode = HttpStatusCode.OK, body: String = "{}") = StaffRepositoryImpl(StaffApi(client(status, body)))
    private fun validation(body: String = """{"status":"VALID"}""") = ValidationRepositoryImpl(TicketsApi(client(HttpStatusCode.OK, body)))
    private fun Result<*>.error() = (exceptionOrNull() as ApiException).error

    @Test
    fun `staffing events map and dedupe, and a nameless row is dropped`() = runTest {
        val events = staff(body = """[{"id":"$eventId","name":"Summer Vibes"},{"id":"$eventId","name":"Summer Vibes"},{"id":"$eventId"}]""")
            .staffingEvents().getOrThrow()
        assertEquals("/api/v1/users/me/staffing-events", requests.single().url.encodedPath)
        assertEquals(listOf("Summer Vibes"), events.map { it.name })
    }

    @Test
    fun `an invite code is sent normalized, the way people type it`() = runTest {
        val joined = staff(body = """{"eventId":"$eventId","eventName":"Summer Vibes"}""").acceptInvite(" k7q2m-9xh4p ").getOrThrow()
        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/v1/staff-invites/K7Q2M9XH4P/accept", request.url.encodedPath)
        assertEquals(JoinedEvent(eventId, "Summer Vibes"), joined)
    }

    @Test
    fun `a malformed invite code never reaches the network`() = runTest {
        assertEquals(ApiError.NotFound, staff().acceptInvite("../admin").error())
        assertEquals(ApiError.NotFound, staff().acceptInvite("K7Q2M").error())
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `used and expired invites map to Conflict and Gone`() = runTest {
        assertEquals(ApiError.Conflict, staff(HttpStatusCode.Conflict, """{"code":"INVITE_USED"}""").acceptInvite("K7Q2M-9XH4P").error())
        assertEquals(ApiError.Gone, staff(HttpStatusCode.Gone, """{"code":"INVITE_EXPIRED"}""").acceptInvite("K7Q2M-9XH4P").error())
    }

    @Test
    fun `guest search sends the query and skips short ones`() = runTest {
        val body = """[{"ticketId":"t-1","ticketCode":"F5A3-038B","attendeeName":"Yash","attendeeEmail":"ya***@gmail.com","ticketTypeName":"VIP","status":"USED"},{"ticketId":null}]"""
        val repo = staff(body = body)
        assertEquals(emptyList<Any>(), repo.searchGuests(eventId, " y ").getOrThrow())
        assertTrue(requests.isEmpty())

        val guests = repo.searchGuests(eventId, "yash").getOrThrow()
        assertEquals("/api/v1/staff/events/$eventId/guests", requests.single().url.encodedPath)
        assertEquals("yash", requests.single().url.parameters["q"])
        assertEquals(1, guests.size) // the row without a ticket id is dropped
        assertEquals(TicketStatus.Used, guests.single().status)
    }

    @Test
    fun `manual check-in sends a code without its dash, or a full id as-is, as MANUAL`() = runTest {
        val repo = validation()
        repo.checkIn(" f5a3-038b ", eventId, key).getOrThrow()
        repo.checkIn("f5a3038b-3412-46f9-8f10-33fc236d6b17", eventId, key).getOrThrow()

        val sent = bodies.map { Json.parseToJsonElement(it).jsonObject }
        assertEquals(listOf("f5a3038b", "f5a3038b-3412-46f9-8f10-33fc236d6b17"), sent.map { it["id"]!!.jsonPrimitive.content })
        assertTrue(sent.all { it["method"]!!.jsonPrimitive.content == "MANUAL" && it["eventId"]!!.jsonPrimitive.content == eventId })
    }

    @Test
    fun `manual entry that is neither a code nor an id is Invalid without a request`() = runTest {
        assertEquals(
            com.venuesync.app.core.model.ScanStatus.Invalid,
            validation().checkIn("hello there", eventId, key).getOrThrow().status,
        )
        assertTrue(requests.isEmpty())
    }
}
