package com.venuesync.app.core.network

import com.venuesync.app.core.auth.SessionManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import java.io.IOException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Server-side dedupe: a request carrying this header can be repeated without doing the work twice. */
const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"

/*
 * Ktor client factory (core/ = pure Kotlin, no android.* imports — KMP-ready boundary).
 */
object VenueSyncHttpClient {

    /** Status codes that mean "try again shortly" (gateway/cold start), not "the server has a bug". */
    private val TransientStatuses = setOf(502, 503, 504)

    fun create(
        baseUrl: String,
        enableLogging: Boolean,
        engine: HttpClientEngine = OkHttp.create(),
        retryBaseDelayMs: Long = 1_000, // tests pass 0
        session: SessionManager? = null, // null = anonymous client (tests)
        /** False when the phone has no network at all (airplane mode); supplied by the app, core stays android-free. */
        hasNetwork: () -> Boolean = { true },
    ): HttpClient = HttpClient(engine) {
        // Non-2xx must throw (ResponseException) — otherwise a 400 ErrorDto would silently
        // deserialize into an empty PageResponse because every field has a default.
        expectSuccess = true
        defaultRequest {
            url(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            )
        }
        // Must be installed before HttpTimeout. Only safe-to-repeat requests:
        // GET by definition, and a request carrying an Idempotency-Key, because the server answers a repeat
        // with the original result (same ticket) instead of doing the work twice. A plain POST never retries.
        install(HttpRequestRetry) {
            maxRetries = 2
            retryIf { request, response ->
                response.status.value in TransientStatuses &&
                    (request.method == HttpMethod.Get || request.headers.contains(IDEMPOTENCY_KEY_HEADER))
            }
            // A request timeout is not retried: Ktor reports it as cancellation, not IOException (RetryPolicyTest
            // pins this). That matters for purchase: the user gets Try again after 60s, not after 3 x 60s.
            // With no network at all a retry can't succeed; it only delays the offline fallback (a saved ticket at the
            // door) by 1s + 2s. A flaky network (venue Wi-Fi) still has a network, so it keeps its retries.
            retryOnExceptionIf { request, cause ->
                cause is IOException && hasNetwork() &&
                    (request.method == HttpMethod.Get || request.headers.contains(IDEMPOTENCY_KEY_HEADER))
            }
            // 1s, then 2s. Retry-After is ignored so a hostile/buggy header can't freeze the screen for an hour.
            delayMillis(respectRetryAfterHeader = false) { retry -> retryBaseDelayMs * retry }
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 30_000 // Render free-tier cold start can take ~30s
        }
        // This client talks to exactly one origin: the API. Anything else is refused before it is sent.
        // Required for token safety — on a 401 from a request that carried no token, Ktor's Auth plugin
        // retries with its cached token WITHOUT consulting refreshTokens, so a foreign host answering
        // 401 would receive the user's access token. No foreign request → no 401 → no leak.
        val origin = Url(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
        install(
            createClientPlugin("SingleOriginGuard") {
                onRequest { request, _ ->
                    val url = request.url
                    check(url.protocol == origin.protocol && url.host == origin.host && url.port == origin.port) {
                        "Blocked request outside the API origin"
                    }
                }
            },
        )
        if (session != null) {
            install(Auth) {
                bearer {
                    // Ktor never sees the refresh token; SessionManager owns it.
                    // NonCancellable: Ktor runs ONE load/refresh for all concurrent requests and hands the starter's
                    // cancellation to every waiter, so a cancelled search (each keystroke cancels the last) killed the
                    // next one too. Finishing it serves the others, and a refresh is never cut off after Auth0 has
                    // already rotated the refresh token (which would lose the new one and end the session).
                    loadTokens {
                        withContext(NonCancellable) { session.currentTokens() }?.let { BearerTokens(it.accessToken, null) }
                    }
                    refreshTokens {
                        val rejected = oldTokens?.accessToken
                        withContext(NonCancellable) { session.refresh(rejected) }?.let { BearerTokens(it.accessToken, null) }
                    }
                    sendWithoutRequest { true } // SingleOriginGuard guarantees every request is to our API
                }
            }
        }
        if (enableLogging) {
            install(Logging) {
                // Logger.DEFAULT goes to SLF4J, which has no provider on Android: every line was silently
                // dropped. ANDROID writes to Logcat (tag "Ktor Client"). INFO = method, URL, status; no bodies.
                logger = Logger.ANDROID
                level = LogLevel.INFO
                sanitizeHeader { it == HttpHeaders.Authorization }
            }
        }
    }
}
