package com.venuesync.app.core.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

sealed interface RefreshResult {
    data class Success(val tokens: TokenResponseDto) : RefreshResult

    /** Auth0 refused the refresh token (revoked, expired, reused): the session is over. */
    data object Rejected : RefreshResult

    /** Network down, timeout, 5xx, 429, garbage body: the session may be fine, try again later. */
    data object Unavailable : RefreshResult
}

@Serializable
data class TokenResponseDto(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null, // rotation: a NEW one every refresh
    @SerialName("id_token") val idToken: String? = null,
)

@Serializable
private data class RevokeRequest(
    @SerialName("client_id") val clientId: String,
    val token: String,
)

/**
 * Auth0's token endpoints. Uses its own HttpClient on purpose: no Auth plugin (a refresh must not
 * itself trigger a refresh) and no logging (these bodies ARE the tokens).
 */
class AuthApi(
    private val client: HttpClient,
    authority: String,
    private val clientId: String,
) {
    private val base = authority.trimEnd('/')

    suspend fun refresh(refreshToken: String): RefreshResult = try {
        val dto = client.post("$base/oauth/token") {
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("grant_type", "refresh_token")
                        append("client_id", clientId)
                        append("refresh_token", refreshToken)
                    },
                ),
            )
        }.body<TokenResponseDto>()
        if (dto.accessToken.isNullOrBlank()) RefreshResult.Unavailable else RefreshResult.Success(dto)
    } catch (e: CancellationException) {
        throw e
    } catch (e: ResponseException) {
        // Auth0 answers invalid_grant with 403; 400/401 also mean "this refresh token is no good".
        if (e.response.status.value in RejectedStatuses) RefreshResult.Rejected else RefreshResult.Unavailable
    } catch (e: Exception) {
        RefreshResult.Unavailable // IO, timeout, unparseable body — never a reason to sign someone out
    }

    /** Best effort: the local sign-out has already happened, a failed revoke must not surface. */
    suspend fun revoke(refreshToken: String) {
        try {
            client.post("$base/oauth/revoke") {
                contentType(ContentType.Application.Json)
                setBody(RevokeRequest(clientId, refreshToken))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // ponytail: an unrevoked refresh token stays valid until Auth0's expiry; no retry queue yet.
        }
    }

    companion object {
        private val RejectedStatuses = setOf(400, 401, 403)

        fun createHttpClient(engine: HttpClientEngine = OkHttp.create()): HttpClient = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 20_000
            }
        }
    }
}
