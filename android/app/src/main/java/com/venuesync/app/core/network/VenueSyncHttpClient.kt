package com.venuesync.app.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import java.io.IOException
import kotlinx.serialization.json.Json

/*
 * Ktor client factory (core/ = pure Kotlin, no android.* imports — KMP-ready boundary).
 * The Auth plugin (bearer token + 401 refresh) is Yash's work in the Jul 19 auth session.
 */
object VenueSyncHttpClient {

    /** Status codes that mean "try again shortly" (gateway/cold start), not "the server has a bug". */
    private val TransientStatuses = setOf(502, 503, 504)

    fun create(
        baseUrl: String,
        enableLogging: Boolean,
        engine: HttpClientEngine = OkHttp.create(),
        retryBaseDelayMs: Long = 1_000, // tests pass 0
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
        // Must be installed before HttpTimeout. GET only: repeating a POST (purchase) could double-charge.
        install(HttpRequestRetry) {
            maxRetries = 2
            retryIf { request, response ->
                request.method == HttpMethod.Get && response.status.value in TransientStatuses
            }
            retryOnExceptionIf { request, cause ->
                request.method == HttpMethod.Get && cause is IOException
            }
            // 1s, then 2s. Retry-After is ignored so a hostile/buggy header can't freeze the screen for an hour.
            delayMillis(respectRetryAfterHeader = false) { retry -> retryBaseDelayMs * retry }
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 30_000 // Render free-tier cold start can take ~30s
        }
        if (enableLogging) {
            install(Logging) {
                level = LogLevel.INFO
                sanitizeHeader { it == HttpHeaders.Authorization }
            }
        }
    }
}
