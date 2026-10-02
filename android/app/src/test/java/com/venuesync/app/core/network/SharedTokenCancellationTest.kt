package com.venuesync.app.core.network

import com.venuesync.app.core.auth.AuthApi
import com.venuesync.app.core.auth.AuthTokens
import com.venuesync.app.core.auth.SessionManager
import com.venuesync.app.core.auth.TokenStore
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Found on a device: typing fast in search left a spinner forever. Every keystroke cancels the previous search, and
 * Ktor shares ONE token load between concurrent requests, handing the starter's cancellation to all of them. The last
 * search, never cancelled itself, died with someone else's CancellationException and the screen never heard back.
 */
class SharedTokenCancellationTest {

    /** Reading the tokens takes a moment (Keystore + DataStore on a device); the test decides when it finishes. */
    private class SlowTokenStore(private val value: AuthTokens) : TokenStore {
        val release = CompletableDeferred<Unit>()
        override val tokens: Flow<AuthTokens?> = flow {
            release.await()
            emit(value)
        }
        override val forceLoginNext = MutableStateFlow(false)
        override suspend fun save(tokens: AuthTokens) = Unit
        override suspend fun clear() = Unit
        override suspend fun setForceLoginNext(value: Boolean) = Unit
    }

    @Test
    fun `cancelling the request that started the token load doesn't cancel the others waiting for it`() = runTest {
        val store = SlowTokenStore(AuthTokens("at1", "rt1", null))
        val session = SessionManager(store, AuthApi(AuthApi.createHttpClient(MockEngine { error("not used") }), "https://t", "c"), backgroundScope)
        val client = VenueSyncHttpClient.create(
            "https://api.example/api/v1",
            enableLogging = false,
            engine = MockEngine { respond("{}", HttpStatusCode.OK) },
            retryBaseDelayMs = 0,
            session = session,
        )

        val first = async { client.get("published-events?q=e") }      // starts the token load
        yield()
        val second = async<HttpResponse> { client.get("published-events?q=ev") } // waits for that same load
        yield()
        first.cancel() // the next keystroke cancels the previous search
        store.release.complete(Unit)

        assertEquals(HttpStatusCode.OK, second.await().status)
    }
}
