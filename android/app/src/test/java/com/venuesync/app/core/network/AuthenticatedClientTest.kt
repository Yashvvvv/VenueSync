package com.venuesync.app.core.network

import com.venuesync.app.core.auth.AuthApi
import com.venuesync.app.core.auth.AuthTokens
import com.venuesync.app.core.auth.SessionManager
import com.venuesync.app.core.auth.FakeTokenStore
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The bearer rules on the real API client: who gets the token, and when it refreshes. */
class AuthenticatedClientTest {

    private val apiRequests = mutableListOf<HttpRequestData>()
    private var auth0Calls = 0

    /** [apiStatus] decides the API's answer per request, given the Authorization header it received. */
    private fun CoroutineScope.client(
        tokens: AuthTokens?,
        apiStatus: (String?) -> HttpStatusCode = { HttpStatusCode.OK },
    ): Pair<io.ktor.client.HttpClient, FakeTokenStore> {
        val store = FakeTokenStore(tokens)
        val auth0 = MockEngine {
            auth0Calls++
            respond("""{"access_token":"at2","refresh_token":"rt2"}""", HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val session = SessionManager(store, AuthApi(AuthApi.createHttpClient(auth0), "https://tenant.example", "c"), this)
        val api = MockEngine { request ->
            apiRequests += request
            respond("{}", apiStatus(request.headers[HttpHeaders.Authorization]), headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = VenueSyncHttpClient.create(
            "https://api.example/api/v1", enableLogging = false, engine = api, retryBaseDelayMs = 0, session = session,
        )
        return client to store
    }

    private val signedIn = AuthTokens("at1", "rt1", null)

    @Test
    fun `signed-in requests to our API carry the access token`() = runTest {
        val (client, _) = backgroundScope.client(signedIn)
        client.get("published-events")
        assertEquals("Bearer at1", apiRequests.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun `signed-out requests carry no token`() = runTest {
        val (client, _) = backgroundScope.client(null)
        client.get("published-events")
        assertNull(apiRequests.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun `expired token is refreshed once and the request retried`() = runTest {
        val (client, store) = backgroundScope.client(signedIn) { header ->
            if (header == "Bearer at1") HttpStatusCode.Unauthorized else HttpStatusCode.OK
        }
        client.get("tickets")
        assertEquals(listOf("Bearer at1", "Bearer at2"), apiRequests.map { it.headers[HttpHeaders.Authorization] })
        assertEquals(1, auth0Calls)
        assertEquals("rt2", store.tokens.value?.refreshToken)
    }

    @Test
    fun `requests to other hosts are refused before anything is sent`() = runTest {
        val (client, _) = backgroundScope.client(signedIn) { HttpStatusCode.Unauthorized }
        val error = runCatching { client.get("https://evil.example/steal") }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
        assertEquals(0, apiRequests.size) // nothing reached the network, so no 401 retry can leak the token
        assertEquals(0, auth0Calls)
    }

    @Test
    fun `plain http to our own host is refused`() = runTest {
        val (client, _) = backgroundScope.client(signedIn)
        assertTrue(runCatching { client.get("http://api.example/api/v1/published-events") }.exceptionOrNull() is IllegalStateException)
        assertEquals(0, apiRequests.size)
    }

    @Test
    fun `a different port on our host is refused`() = runTest {
        val (client, _) = backgroundScope.client(signedIn)
        assertTrue(runCatching { client.get("https://api.example:8443/api/v1/x") }.exceptionOrNull() is IllegalStateException)
        assertEquals(0, apiRequests.size)
    }
}
