package com.venuesync.app.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.venuesync.app.BuildConfig
import com.venuesync.app.core.auth.AuthTokens
import com.venuesync.app.core.auth.roles
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenResponse

sealed interface LoginResult {
    data class Success(val tokens: AuthTokens) : LoginResult

    /** The user closed the browser tab. Not an error — show nothing. */
    data object Cancelled : LoginResult
    data class Failed(val reason: LoginFailure) : LoginResult
}

enum class LoginFailure { Network, DeviceClock, Rejected }

/**
 * Auth0 login through AppAuth: system browser tab (never a WebView), PKCE, state and nonce are all
 * generated and checked by the library. A forged or mismatched redirect comes back as an
 * AuthorizationException and lands in [LoginFailure.Rejected].
 */
@Singleton
class AuthFlow @Inject constructor(
    @ApplicationContext context: Context,
) {
    // ponytail: one AuthorizationService for the process instead of create/dispose per Activity;
    // fine for a single-Activity app, revisit if more Activities start logins.
    private val service = AuthorizationService(context)
    private var config: AuthorizationServiceConfiguration? = null

    /** Throws [AuthorizationException] when Auth0's discovery document can't be fetched. */
    suspend fun loginIntent(forceLogin: Boolean): Intent {
        val request = AuthorizationRequest.Builder(
            config(),
            BuildConfig.OIDC_CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse(BuildConfig.OIDC_REDIRECT_URI),
        )
            .setScopes("openid", "profile", "email", "offline_access") // offline_access → a refresh token
            // Without the audience Auth0 issues an opaque token the API can't validate.
            .setAdditionalParameters(mapOf("audience" to BuildConfig.OIDC_AUDIENCE))
            .apply { if (forceLogin) setPrompt(AuthorizationRequest.Prompt.LOGIN) }
            .build()
        if (BuildConfig.DEBUG) Log.d(TAG, "Opening Auth0 login (forceLogin=$forceLogin)")
        return service.getAuthorizationRequestIntent(request)
    }

    /** Turns the browser tab's result into tokens. Every outcome is mapped; nothing throws. */
    suspend fun completeLogin(data: Intent?): LoginResult {
        if (data == null) return LoginResult.Cancelled.also { if (BuildConfig.DEBUG) Log.d(TAG, "Login cancelled") }
        val response = AuthorizationResponse.fromIntent(data)
        val error = AuthorizationException.fromIntent(data)
        if (response == null) {
            return when (error) {
                null, AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW -> LoginResult.Cancelled
                    .also { if (BuildConfig.DEBUG) Log.d(TAG, "Login cancelled") }
                else -> LoginResult.Failed(error.toFailure())
            }
        }
        return try {
            val tokens = exchange(response)
            val accessToken = tokens.accessToken?.takeIf { it.isNotBlank() }
                ?: return LoginResult.Failed(LoginFailure.Rejected)
            if (tokens.refreshToken == null) {
                Log.w(TAG, "No refresh token issued: is 'Allow Offline Access' on for the Auth0 API?")
            }
            LoginResult.Success(AuthTokens(accessToken, tokens.refreshToken, tokens.idToken)).also {
                // Flow facts only, never token values. Empty roles here = audience not applied.
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Login OK: jwt=${accessToken.count { c -> c == '.' } == 2}, " +
                        "refreshToken=${tokens.refreshToken != null}, roles=${it.tokens.roles()}")
                }
            }
        } catch (e: AuthorizationException) {
            LoginResult.Failed(e.toFailure())
        }
    }

    private fun AuthorizationException.toFailure(): LoginFailure {
        Log.w(TAG, "Login failed: type=$type code=$code error=$error") // codes only; never tokens
        return when (this) {
            AuthorizationException.GeneralErrors.NETWORK_ERROR,
            AuthorizationException.GeneralErrors.SERVER_ERROR,
            -> LoginFailure.Network
            // Almost always a wrong device clock: the token looks issued in the future or long expired.
            AuthorizationException.GeneralErrors.ID_TOKEN_VALIDATION_ERROR -> LoginFailure.DeviceClock
            else -> LoginFailure.Rejected
        }
    }

    private suspend fun config(): AuthorizationServiceConfiguration =
        config ?: suspendCancellableCoroutine { cont ->
            AuthorizationServiceConfiguration.fetchFromIssuer(Uri.parse(BuildConfig.OIDC_AUTHORITY)) { cfg, ex ->
                if (!cont.isActive) return@fetchFromIssuer
                if (cfg != null) cont.resume(cfg)
                else cont.resumeWithException(ex ?: AuthorizationException.GeneralErrors.NETWORK_ERROR)
            }
        }.also { config = it }

    private suspend fun exchange(response: AuthorizationResponse): TokenResponse =
        suspendCancellableCoroutine { cont ->
            service.performTokenRequest(response.createTokenExchangeRequest()) { tokens, ex ->
                if (!cont.isActive) return@performTokenRequest
                if (tokens != null) cont.resume(tokens)
                else cont.resumeWithException(ex ?: AuthorizationException.GeneralErrors.SERVER_ERROR)
            }
        }

    private companion object {
        const val TAG = "AuthFlow"
    }
}
