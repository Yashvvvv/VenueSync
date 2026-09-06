package com.venuesync.app.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/*
 * Ktor client factory (core/ = pure Kotlin, no android.* imports — KMP-ready boundary).
 * The Auth plugin (bearer token + 401 refresh) is Yash's work in the Jul 19 auth session.
 */
object VenueSyncHttpClient {

    fun create(baseUrl: String): HttpClient = HttpClient(OkHttp) {
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
        install(Logging) {
            level = LogLevel.INFO
        }
    }
}
