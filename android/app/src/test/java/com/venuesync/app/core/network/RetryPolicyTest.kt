package com.venuesync.app.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** Which requests the client may repeat on its own. Repeating the wrong one could buy a ticket twice. */
class RetryPolicyTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun client(hasNetwork: Boolean = true, handler: suspend () -> HttpStatusCode): HttpClient = VenueSyncHttpClient.create(
        "https://api.example/api/v1",
        enableLogging = false,
        engine = MockEngine { request ->
            requests += request
            respond("", handler())
        },
        retryBaseDelayMs = 0,
        hasNetwork = { hasNetwork },
    )

    private suspend fun HttpClient.attempt(block: suspend HttpClient.() -> Unit) = runCatching { block() }

    @Test
    fun `a connection failure is retried while there is a network`() = runBlocking {
        client { throw IOException("reset") }.attempt { get("x") }
        assertEquals(3, requests.size)
    }

    @Test
    fun `with no network at all a failure is final at once, so the offline copy shows without waiting`() = runBlocking {
        client(hasNetwork = false) { throw IOException("Unable to resolve host") }.attempt { get("x") }
        assertEquals(1, requests.size)
    }

    @Test
    fun `POST without a key is never retried`() = runBlocking {
        client { HttpStatusCode.ServiceUnavailable }.attempt { post("x") }
        assertEquals(1, requests.size)
    }

    @Test
    fun `POST with a key is retried on 503 with the same key`() = runBlocking {
        client { HttpStatusCode.ServiceUnavailable }.attempt { post("x") { header(IDEMPOTENCY_KEY_HEADER, "k1") } }
        assertEquals(3, requests.size)
        assertEquals(listOf("k1", "k1", "k1"), requests.map { it.headers[IDEMPOTENCY_KEY_HEADER] })
    }

    @Test
    fun `POST with a key is retried on a connection failure`() = runBlocking {
        client { throw IOException("reset") }.attempt { post("x") { header(IDEMPOTENCY_KEY_HEADER, "k1") } }
        assertEquals(3, requests.size)
    }

    @Test
    fun `POST with a key is not retried after its request timeout`() = runBlocking {
        client { delay(5_000); HttpStatusCode.OK }.attempt {
            post("x") {
                header(IDEMPOTENCY_KEY_HEADER, "k1")
                timeout { requestTimeoutMillis = 50 }
            }
        }
        assertEquals(1, requests.size)
    }
}
