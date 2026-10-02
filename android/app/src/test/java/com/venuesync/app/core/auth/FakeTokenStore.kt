package com.venuesync.app.core.auth

import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [TokenStore]: there's no Keystore on a JVM. */
internal class FakeTokenStore(initial: AuthTokens? = null) : TokenStore {
    override val tokens = MutableStateFlow(initial)
    override val forceLoginNext = MutableStateFlow(false)
    override suspend fun save(tokens: AuthTokens) { this.tokens.value = tokens }
    override suspend fun clear() { tokens.value = null }
    override suspend fun setForceLoginNext(value: Boolean) { forceLoginNext.value = value }
}
