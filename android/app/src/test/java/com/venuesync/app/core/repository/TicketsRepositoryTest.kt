package com.venuesync.app.core.repository

import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.TicketStatus
import com.venuesync.app.core.network.IDEMPOTENCY_KEY_HEADER
import com.venuesync.app.core.network.TicketsApi
import com.venuesync.app.core.network.VenueSyncHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Drives the real client config (expectSuccess, retry, JSON) against a fake server. */
class TicketsRepositoryTest {

    private val eventId = "3f2b8c1e-9a4d-4e6f-8b7a-1c2d3e4f5a6b"
    private val typeId = "6c1d2e3f-4a5b-4c6d-8e7f-9a0b1c2d3e4f"
    private val key = "0b1c2d3e-4f5a-4b6c-9d8e-7f6a5b4c3d2e"
    private val ticketId = "9e8d7c6b-5a4f-4e3d-a2c1-b0a9f8e7d6c5"
    private val requests = mutableListOf<HttpRequestData>()

    private fun ticketJson(forEvent: String = eventId) =
        """{"id":"$ticketId","status":"PURCHASED","ticketTypeName":"GA","price":25.0,"eventId":"$forEvent","eventName":"Show"}"""

    private fun repo(handler: suspend () -> Pair<HttpStatusCode, String>): TicketsRepository {
        val engine = MockEngine { request ->
            requests += request
            val (status, body) = handler()
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = VenueSyncHttpClient.create(
            "https://example.test/api/v1",
            enableLogging = false,
            engine = engine,
            retryBaseDelayMs = 0,
        )
        return TicketsRepositoryImpl(TicketsApi(client), FakeTicketCache(), NoBackgroundWork)
    }

    private suspend fun TicketsRepository.errorFor(
        event: String = eventId,
        type: String = typeId,
        idempotencyKey: String = key,
    ): ApiError = (purchase(event, type, idempotencyKey).exceptionOrNull() as ApiException).error

    private fun failing(status: HttpStatusCode, code: String? = null) =
        repo { status to (code?.let { """{"code":"$it","error":"server text"}""" } ?: "") }

    @Test
    fun `sends POST with the Idempotency-Key and maps the ticket`() = runTest {
        val ticket = repo { HttpStatusCode.Created to ticketJson() }.purchase(eventId, typeId, key).getOrThrow()
        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/v1/events/$eventId/ticket-types/$typeId/tickets", request.url.encodedPath)
        assertEquals(key, request.headers[IDEMPOTENCY_KEY_HEADER])
        assertEquals(ticketId, ticket.id)
        assertEquals(TicketStatus.Purchased, ticket.status)
    }

    @Test
    fun `503 then 201 retries with the same key`() = runTest {
        val repo = repo {
            if (requests.size == 1) HttpStatusCode.ServiceUnavailable to "" else HttpStatusCode.Created to ticketJson()
        }
        assertEquals(ticketId, repo.purchase(eventId, typeId, key).getOrThrow().id)
        assertEquals(listOf(key, key), requests.map { it.headers[IDEMPOTENCY_KEY_HEADER] })
    }

    @Test
    fun `IO failure is retried with the same key, then maps to Network`() = runTest {
        assertEquals(ApiError.Network, repo { throw IOException("offline") }.errorFor())
        assertEquals(listOf(key, key, key), requests.map { it.headers[IDEMPOTENCY_KEY_HEADER] })
    }

    @Test
    fun `error codes win over the status`() = runTest {
        assertEquals(ApiError.SoldOut, failing(HttpStatusCode.Conflict, "TICKETS_SOLD_OUT").errorFor())
        assertEquals(ApiError.NotOnSale, failing(HttpStatusCode.Conflict, "SALES_NOT_STARTED").errorFor())
        assertEquals(ApiError.NotOnSale, failing(HttpStatusCode.Conflict, "SALES_ENDED").errorFor())
    }

    @Test
    fun `unknown code falls back to the status`() = runTest {
        assertEquals(ApiError.Conflict, failing(HttpStatusCode.Conflict, "SOMETHING_NEW").errorFor())
        assertEquals(ApiError.Unknown("server text"), failing(HttpStatusCode.BadRequest, "SOMETHING_NEW").errorFor())
        assertEquals(ApiError.NotFound, failing(HttpStatusCode.NotFound, "TICKET_TYPE_NOT_FOUND").errorFor())
    }

    @Test
    fun `403 without a body maps to Forbidden`() = runTest {
        assertEquals(ApiError.Forbidden, failing(HttpStatusCode.Forbidden).errorFor())
    }

    @Test
    fun `invalid ids or key never reach the network`() = runTest {
        val repo = repo { error("must not be called") }
        assertEquals(ApiError.NotFound, repo.errorFor(event = "../admin"))
        assertEquals(ApiError.NotFound, repo.errorFor(type = "t1"))
        assertEquals(ApiError.Unknown("invalid idempotency key"), repo.errorFor(idempotencyKey = "k1"))
        assertEquals(0, requests.size)
    }

    @Test
    fun `ticket for a different event maps to InvalidResponse`() = runTest {
        val other = "00000000-0000-0000-0000-000000000000"
        assertEquals(ApiError.InvalidResponse, repo { HttpStatusCode.Created to ticketJson(forEvent = other) }.errorFor())
    }

    @Test
    fun `ticket without required fields maps to InvalidResponse`() = runTest {
        assertEquals(ApiError.InvalidResponse, repo { HttpStatusCode.Created to """{"id":"$ticketId"}""" }.errorFor())
    }
}
