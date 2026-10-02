package com.venuesync.app.core.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface Session {
    /** Storage not read yet — the UI shows neither "Sign in" nor the account menu. */
    data object Unknown : Session
    data object SignedOut : Session
    data class SignedIn(val roles: Set<String>) : Session
}

/**
 * The single owner of the auth session. Every token write goes through [mutex], so sign-in,
 * sign-out and refresh can never interleave (e.g. a slow refresh resurrecting a signed-out session).
 */
class SessionManager(
    private val store: TokenStore,
    private val api: AuthApi,
    scope: CoroutineScope,
) {
    val session: StateFlow<Session> = store.tokens
        .map { if (it == null) Session.SignedOut else Session.SignedIn(it.roles()) }
        .stateIn(scope, SharingStarted.Eagerly, Session.Unknown)

    private val mutex = Mutex()

    suspend fun currentTokens(): AuthTokens? = store.tokens.first()

    suspend fun shouldForceLogin(): Boolean = store.forceLoginNext.first()

    suspend fun signIn(tokens: AuthTokens) = mutex.withLock {
        store.save(tokens)
        store.setForceLoginNext(false)
    }

    /** Local sign-out always completes; revoking the refresh token at Auth0 is best effort afterwards. */
    suspend fun signOut() {
        val refreshToken = mutex.withLock {
            val rt = currentTokens()?.refreshToken
            store.clear()
            store.setForceLoginNext(true) // Auth0's browser cookie would otherwise sign the same user straight back in
            rt
        }
        refreshToken?.let { api.revoke(it) }
    }

    /**
     * Called by the HTTP client after a 401. [failedAccessToken] is the token the server just rejected.
     *
     * Refresh tokens rotate: each one works exactly once, and Auth0 treats a second use as theft and
     * kills the whole session. So concurrent 401s must produce exactly ONE refresh — the mutex plus
     * the "already refreshed?" check guarantee it, regardless of what the HTTP library does.
     */
    suspend fun refresh(failedAccessToken: String?): AuthTokens? = mutex.withLock {
        val stored = currentTokens() ?: return@withLock null // signed out meanwhile
        if (stored.accessToken != failedAccessToken) return@withLock stored // another caller already refreshed
        val refreshToken = stored.refreshToken ?: run {
            store.clear() // expired access token and no way to renew it: the session is over
            return@withLock null
        }
        when (val result = api.refresh(refreshToken)) {
            is RefreshResult.Success -> AuthTokens(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken ?: refreshToken,
                idToken = result.idToken ?: stored.idToken,
            ).also { store.save(it) }
            RefreshResult.Rejected -> {
                store.clear()
                null
            }
            RefreshResult.Unavailable -> null // keep the session; only this request fails
        }
    }
}
