package com.venuesync.app.core.repository

import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.ScanResult
import com.venuesync.app.core.model.ScanStatus
import com.venuesync.app.core.network.IDEMPOTENCY_KEY_HEADER
import com.venuesync.app.core.network.TicketsApi
import com.venuesync.app.core.network.VenueSyncHttpClient
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

class ValidationRepositoryTest {

    private val qr = "0b1c2d3e-4f5a-4b6c-9d8e-7f6a5b4c3d2e"
    private val eventId = "3f2b8c1e-9a4d-4e6f-8b7a-1c2d3e4f5a6b"
    private val key = "9e8d7c6b-5a4f-4e3d-a2c1-b0a9f8e7d6c5"
    private val requests = mutableListOf<HttpRequestData>()
    private val bodies = mutableListOf<String>()

    private fun repo(handler: suspend () -> Pair<HttpStatusCode, String>) = ValidationRepositoryImpl(
        TicketsApi(
            VenueSyncHttpClient.create(
                "https://example.test/api/v1",
                enableLogging = false,
                engine = MockEngine { request ->
                    requests += request
                    bodies += String(request.body.toByteArray())
                    val (status, body) = handler()
                    respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
                },
                retryBaseDelayMs = 0,
            ),
        ),
    )

    private fun answer(status: String, extra: String = "") = repo { HttpStatusCode.OK to """{"status":"$status"$extra}""" }

    @Test
    fun `posts the code, method and event with the key`() = runTest {
        val result = answer("VALID", ""","eventName":"Show","ticketTypeName":"VIP"""").validate(qr, eventId, key).getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/v1/ticket-validations", request.url.encodedPath)
        assertEquals(key, request.headers[IDEMPOTENCY_KEY_HEADER])
        val body = Json.parseToJsonElement(bodies.single()).jsonObject
        assertEquals(qr, body["id"]!!.jsonPrimitive.content)
        assertEquals("QR_SCAN", body["method"]!!.jsonPrimitive.content) // the server rejects a body without it
        assertEquals(eventId, body["eventId"]!!.jsonPrimitive.content)
        assertEquals(ScanResult(ScanStatus.Valid, ticketTypeName = "VIP", eventName = "Show"), result)
    }

    @Test
    fun `every status maps, and an unknown one is Unknown`() = runTest {
        val expected = mapOf(
            "ALREADY_USED" to ScanStatus.AlreadyUsed,
            "EXPIRED" to ScanStatus.Expired,
            "INVALID" to ScanStatus.Invalid,
            "WRONG_EVENT" to ScanStatus.WrongEvent,
            "SOMETHING_NEW" to ScanStatus.Unknown,
        )
        expected.forEach { (wire, status) -> assertEquals(status, answer(wire).validate(qr, eventId, key).getOrThrow().status) }
    }

    @Test
    fun `a code that isn't a UUID is Invalid without asking the server`() = runTest {
        val repo = repo { error("must not be called") }
        assertEquals(ScanStatus.Invalid, repo.validate("https://example.com/promo", eventId, key).getOrThrow().status)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `bad event id never reaches the network`() = runTest {
        val error = (repo { error("must not be called") }.validate(qr, "../events", key).exceptionOrNull() as ApiException).error
        assertEquals(ApiError.NotFound, error)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `503 is retried with the same key`() = runTest {
        val repo = repo { if (requests.size == 1) HttpStatusCode.ServiceUnavailable to "" else HttpStatusCode.OK to """{"status":"VALID"}""" }
        assertEquals(ScanStatus.Valid, repo.validate(qr, eventId, key).getOrThrow().status)
        assertEquals(listOf(key, key), requests.map { it.headers[IDEMPOTENCY_KEY_HEADER] })
    }

    @Test
    fun `an answer without a status is InvalidResponse`() = runTest {
        val error = (repo { HttpStatusCode.OK to "{}" }.validate(qr, eventId, key).exceptionOrNull() as ApiException).error
        assertEquals(ApiError.InvalidResponse, error)
    }
}
