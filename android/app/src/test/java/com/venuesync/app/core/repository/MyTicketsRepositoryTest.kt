package com.venuesync.app.core.repository

import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.TicketFilter
import com.venuesync.app.core.model.TicketStatus
import com.venuesync.app.core.network.TicketsApi
import com.venuesync.app.core.network.VenueSyncHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The read side of tickets: My tickets list, one ticket, its QR code. Real client config, fake server. */
class MyTicketsRepositoryTest {

    private val ticketId = "9e8d7c6b-5a4f-4e3d-a2c1-b0a9f8e7d6c5"
    private val requests = mutableListOf<HttpRequestData>()
    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3)

    private fun repo(status: HttpStatusCode = HttpStatusCode.OK, contentType: String = "application/json", body: () -> ByteArray) =
        TicketsRepositoryImpl(
            TicketsApi(
                VenueSyncHttpClient.create(
                    "https://example.test/api/v1",
                    enableLogging = false,
                    engine = MockEngine { request ->
                        requests += request
                        respond(body(), status, headersOf(HttpHeaders.ContentType, contentType))
                    },
                    retryBaseDelayMs = 0,
                ),
            ),
            FakeTicketCache(),
            NoBackgroundWork,
        )

    private fun json(text: String) = repo { text.toByteArray() }
    private fun Result<*>.error() = (exceptionOrNull() as ApiException).error

    @Test
    fun `list sends filter and page, drops broken rows and keeps the rest`() = runTest {
        val repo = json(
            """{"content":[
                {"id":"a","status":"PURCHASED","ticketType":{"name":"GA"},"eventName":"Show","eventStart":"2026-10-05T19:00:00+05:30"},
                {"id":"b","status":"USED","ticketType":null,"eventName":"Broken"},
                {"id":"c","status":"SOMETHING_NEW","ticketType":{"name":"VIP"},"eventName":"Other"}
            ],"last":false}""",
        )
        val page = repo.listTickets(TicketFilter.Past, page = 2).getOrThrow()

        val url = requests.single().url
        assertEquals("/api/v1/tickets", url.encodedPath)
        assertEquals("past", url.parameters["filter"])
        assertEquals("2", url.parameters["page"])
        assertEquals(listOf("a", "c"), page.tickets.map { it.id })
        assertEquals(TicketStatus.Unknown, page.tickets[1].status) // a new server status never breaks the list
        assertEquals(false, page.isLast)
    }

    @Test
    fun `ticket maps, and a different ticket than asked is InvalidResponse`() = runTest {
        val body = """{"id":"$ticketId","status":"PURCHASED","ticketTypeName":"GA","eventId":"e","eventName":"Show"}"""
        assertEquals(ticketId, json(body).getTicket(ticketId).getOrThrow().id)

        val other = "00000000-0000-0000-0000-000000000000"
        assertEquals(ApiError.InvalidResponse, json(body).getTicket(other).error())
    }

    @Test
    fun `invalid ids never reach the network`() = runTest {
        val repo = json("{}")
        assertEquals(ApiError.NotFound, repo.getTicket("../admin").error())
        assertEquals(ApiError.NotFound, repo.getQrCode("1-1-1-1-1").error())
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `QR asks for a PNG and returns its bytes`() = runTest {
        val bytes = repo(contentType = "image/png") { png }.getQrCode(ticketId).getOrThrow()
        assertArrayEquals(png, bytes)
        assertEquals("/api/v1/tickets/$ticketId/qr-codes", requests.single().url.encodedPath)
        assertTrue(requests.single().headers.getAll(HttpHeaders.Accept).orEmpty().any { "image/png" in it })
    }

    @Test
    fun `QR that is not a PNG, or is huge, is InvalidResponse`() = runTest {
        assertEquals(ApiError.InvalidResponse, repo(contentType = "image/png") { "<html>".toByteArray() }.getQrCode(ticketId).error())
        assertEquals(ApiError.InvalidResponse, repo(contentType = "image/png") { png + ByteArray(1_000_001) }.getQrCode(ticketId).error())
    }

    @Test
    fun `QR 404 maps to NotFound`() = runTest {
        val repo = repo(HttpStatusCode.NotFound) { """{"code":"QR_CODE_NOT_FOUND","error":"x"}""".toByteArray() }
        assertEquals(ApiError.NotFound, repo.getQrCode(ticketId).error())
    }
}
