package com.venuesync.app.core.repository

import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.network.EventsApi
import com.venuesync.app.core.network.VenueSyncHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Drives the real client config (expectSuccess, retry, JSON) against a fake server. */
class EventsRepositoryTest {

    private val id = "3f2b8c1e-9a4d-4e6f-8b7a-1c2d3e4f5a6b"
    private var requests = 0

    private fun repo(handler: suspend () -> Pair<HttpStatusCode, String>): EventsRepository {
        val engine = MockEngine { request ->
            requests++
            assertEquals("/api/v1/published-events/$id", request.url.encodedPath)
            val (status, body) = handler()
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = VenueSyncHttpClient.create(
            "https://example.test/api/v1",
            enableLogging = false,
            engine = engine,
            retryBaseDelayMs = 0,
        )
        return EventsRepositoryImpl(EventsApi(client))
    }

    private suspend fun EventsRepository.errorFor(eventId: String = id): ApiError =
        (getPublishedEvent(eventId).exceptionOrNull() as ApiException).error

    @Test
    fun `valid response maps to domain`() = runTest {
        val repo = repo {
            HttpStatusCode.OK to """{"id":"$id","name":"Show","ticketTypes":[{"id":"t1","name":"GA","price":25.5}]}"""
        }
        val event = repo.getPublishedEvent(id).getOrThrow()
        assertEquals("Show", event.name)
        assertEquals("GA", event.ticketTypes.single().name)
    }

    @Test
    fun `invalid id never reaches the network`() = runTest {
        val repo = repo { error("must not be called") }
        assertEquals(ApiError.NotFound, repo.errorFor("../admin"))
        assertEquals(ApiError.NotFound, repo.errorFor("1-1-1-1-1"))
        assertEquals(0, requests)
    }

    @Test
    fun `404 maps to NotFound`() = runTest {
        assertEquals(ApiError.NotFound, repo { HttpStatusCode.NotFound to "" }.errorFor())
    }

    @Test
    fun `429 maps to RateLimited`() = runTest {
        assertEquals(ApiError.RateLimited, repo { HttpStatusCode.TooManyRequests to """{"error":"slow down"}""" }.errorFor())
    }

    @Test
    fun `500 is not retried`() = runTest {
        val error = repo { HttpStatusCode.InternalServerError to """{"error":"boom"}""" }.errorFor()
        assertEquals(ApiError.Server("boom"), error)
        assertEquals(1, requests)
    }

    @Test
    fun `503 is retried twice then fails`() = runTest {
        repo { HttpStatusCode.ServiceUnavailable to "" }.errorFor()
        assertEquals(3, requests)
    }

    @Test
    fun `503 then success recovers transparently`() = runTest {
        val repo = repo {
            if (requests == 1) HttpStatusCode.ServiceUnavailable to ""
            else HttpStatusCode.OK to """{"id":"$id","name":"Show"}"""
        }
        assertEquals("Show", repo.getPublishedEvent(id).getOrThrow().name)
    }

    @Test
    fun `IO failure is retried then maps to Network`() = runTest {
        assertEquals(ApiError.Network, repo { throw IOException("offline") }.errorFor())
        assertEquals(3, requests)
    }

    @Test
    fun `broken JSON maps to InvalidResponse`() = runTest {
        assertEquals(ApiError.InvalidResponse, repo { HttpStatusCode.OK to """{"id":""" }.errorFor())
    }

    @Test
    fun `wrong JSON type maps to InvalidResponse`() = runTest {
        assertEquals(ApiError.InvalidResponse, repo { HttpStatusCode.OK to """{"id":"$id","name":"Show","ticketTypes":"nope"}""" }.errorFor())
    }

    @Test
    fun `missing required field maps to InvalidResponse`() = runTest {
        assertEquals(ApiError.InvalidResponse, repo { HttpStatusCode.OK to """{"id":"$id"}""" }.errorFor())
    }

    @Test
    fun `response for a different event maps to InvalidResponse`() = runTest {
        val other = "00000000-0000-0000-0000-000000000000"
        assertEquals(ApiError.InvalidResponse, repo { HttpStatusCode.OK to """{"id":"$other","name":"Other"}""" }.errorFor())
    }
}
