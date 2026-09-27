package com.venuesync.app.core.auth

import kotlinx.coroutines.flow.Flow

/**
 * Where the session's tokens live. An interface because it has two implementations: the
 * Keystore-backed one on device, and an in-memory fake in unit tests (no Keystore on a JVM).
 */
interface TokenStore {
    /** Current tokens, or null when signed out or when stored data can't be read back. */
    val tokens: Flow<AuthTokens?>
    suspend fun save(tokens: AuthTokens)
    suspend fun clear()

    /** Set by sign-out so the next login shows Auth0's password prompt instead of reusing its browser cookie. */
    val forceLoginNext: Flow<Boolean>
    suspend fun setForceLoginNext(value: Boolean)
}
