package com.venuesync.app.core.auth

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import java.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {

    private class FakeTokenStore(initial: AuthTokens? = null) : TokenStore {
        override val tokens = MutableStateFlow(initial)
        override val forceLoginNext = MutableStateFlow(false)
        override suspend fun save(tokens: AuthTokens) { this.tokens.value = tokens }
        override suspend fun clear() { tokens.value = null }
        override suspend fun setForceLoginNext(value: Boolean) { forceLoginNext.value = value }
    }

    private val requests = mutableListOf<HttpRequestData>()

    private fun api(status: HttpStatusCode = HttpStatusCode.OK, body: String = "", fail: Boolean = false): AuthApi {
        val engine = MockEngine { request ->
            requests += request
            if (fail) throw IOException("offline")
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return AuthApi(AuthApi.createHttpClient(engine), "https://tenant.example", "client-1")
    }

    private val rotated = """{"access_token":"at2","refresh_token":"rt2"}"""
    private val signedIn = AuthTokens("at1", "rt1", "id1")

    private fun CoroutineScope.manager(store: TokenStore, api: AuthApi) = SessionManager(store, api, this)

    @Test
    fun `refresh saves the rotated tokens`() = runTest {
        val store = FakeTokenStore(signedIn)
        val result = backgroundScope.manager(store, api(body = rotated)).refresh("at1")
        assertEquals(AuthTokens("at2", "rt2", "id1"), result)
        assertEquals(result, store.tokens.value)
    }

    @Test
    fun `refresh keeps the old refresh token when Auth0 does not rotate it`() = runTest {
        val store = FakeTokenStore(signedIn)
        backgroundScope.manager(store, api(body = """{"access_token":"at2"}""")).refresh("at1")
        assertEquals("rt1", store.tokens.value?.refreshToken)
    }

    @Test
    fun `rejected refresh signs out`() = runTest {
        val store = FakeTokenStore(signedIn)
        val result = backgroundScope.manager(store, api(HttpStatusCode.Forbidden, """{"error":"invalid_grant"}""")).refresh("at1")
        assertNull(result)
        assertNull(store.tokens.value)
    }

    @Test
    fun `network failure keeps the session`() = runTest {
        val store = FakeTokenStore(signedIn)
        assertNull(backgroundScope.manager(store, api(fail = true)).refresh("at1"))
        assertEquals(signedIn, store.tokens.value)

        assertNull(backgroundScope.manager(store, api(HttpStatusCode.ServiceUnavailable)).refresh("at1"))
        assertEquals(signedIn, store.tokens.value)
    }

    @Test
    fun `concurrent 401s trigger exactly one refresh`() = runTest {
        val store = FakeTokenStore(signedIn)
        val manager = backgroundScope.manager(store, api(body = rotated))

        val results = List(3) { async { manager.refresh("at1") } }.awaitAll()

        assertEquals(1, requests.size) // a second use of rt1 would make Auth0 kill the session
        assertTrue(results.all { it?.accessToken == "at2" })
    }

    @Test
    fun `refresh with an already-replaced token returns the current one without calling Auth0`() = runTest {
        val store = FakeTokenStore(signedIn)
        assertEquals(signedIn, backgroundScope.manager(store, api(body = rotated)).refresh("some-older-token"))
        assertEquals(0, requests.size)
    }

    @Test
    fun `no refresh token means the session is over`() = runTest {
        val store = FakeTokenStore(AuthTokens("at1", null, null))
        assertNull(backgroundScope.manager(store, api(body = rotated)).refresh("at1"))
        assertNull(store.tokens.value)
        assertEquals(0, requests.size)
    }

    @Test
    fun `sign-out clears locally, revokes, and forces a fresh login next time`() = runTest {
        val store = FakeTokenStore(signedIn)
        backgroundScope.manager(store, api()).signOut()

        assertNull(store.tokens.value)
        assertTrue(store.forceLoginNext.value)
        assertEquals("https://tenant.example/oauth/revoke", requests.single().url.toString())
    }

    @Test
    fun `sign-out still completes when revoke fails`() = runTest {
        val store = FakeTokenStore(signedIn)
        backgroundScope.manager(store, api(fail = true)).signOut()
        assertNull(store.tokens.value)
    }

    @Test
    fun `refresh after sign-out does not resurrect the session`() = runTest {
        val store = FakeTokenStore(signedIn)
        val manager = backgroundScope.manager(store, api(body = rotated))
        manager.signOut()
        assertNull(manager.refresh("at1"))
        assertNull(store.tokens.value)
        assertTrue(requests.none { (it.body as? FormDataContent)?.formData?.get("grant_type") == "refresh_token" })
    }

    @Test
    fun `sign-in clears the force-login flag and exposes roles`() = runTest {
        val store = FakeTokenStore().apply { forceLoginNext.value = true }
        val manager = backgroundScope.manager(store, api())
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"$ROLES_CLAIM":["ROLE_ATTENDEE"]}""".toByteArray())

        manager.signIn(AuthTokens("h.$payload.s", "rt", null))

        assertEquals(false, manager.shouldForceLogin())
        assertEquals(Session.SignedIn(setOf("ROLE_ATTENDEE")), manager.session.first { it is Session.SignedIn })
    }
}
