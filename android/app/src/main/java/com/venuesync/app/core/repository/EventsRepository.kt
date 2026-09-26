package com.venuesync.app.core.repository

import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.ErrorDto
import com.venuesync.app.core.model.Event
import com.venuesync.app.core.model.EventDetail
import com.venuesync.app.core.model.toDomain
import com.venuesync.app.core.model.toDomainOrNull
import com.venuesync.app.core.network.EventsApi
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.ContentConvertException
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerializationException

/** A page of domain events plus whether the server has more. */
data class EventPage(val events: List<Event>, val isLast: Boolean)

interface EventsRepository {
    suspend fun getPublishedEvents(query: String? = null, page: Int = 0): Result<EventPage>
    suspend fun getPublishedEvent(id: String): Result<EventDetail>
}

/**
 * The only place in the app that knows HTTP exists — and the trust boundary: input going out
 * is validated, data coming in is validated, every failure becomes an [ApiError].
 * Failures are always an [ApiException] inside [Result.failure]; only cancellation escapes.
 */
class EventsRepositoryImpl @Inject constructor(
    private val api: EventsApi,
) : EventsRepository {

    override suspend fun getPublishedEvents(query: String?, page: Int): Result<EventPage> = call {
        val response = api.getPublishedEvents(query = query, page = page)
        EventPage(events = response.content.map { it.toDomain() }, isLast = response.last)
    }

    override suspend fun getPublishedEvent(id: String): Result<EventDetail> {
        // Ids can come from deep links later; anything that isn't a UUID never reaches the network.
        if (!UuidRegex.matches(id)) return Result.failure(ApiException(ApiError.NotFound))
        return call {
            api.getPublishedEvent(id).toDomainOrNull()
                ?.takeIf { it.id.equals(id, ignoreCase = true) } // server answered the question we asked
                ?: throw ApiException(ApiError.InvalidResponse)
        }
    }

    private suspend fun <T> call(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e // never swallow coroutine cancellation
    } catch (e: ApiException) {
        Result.failure(e) // already classified inside the block
    } catch (e: ResponseException) {
        Result.failure(ApiException(e.toApiError()))
    } catch (e: IOException) {
        Result.failure(ApiException(ApiError.Network))
    } catch (e: ContentConvertException) {
        Result.failure(ApiException(ApiError.InvalidResponse))
    } catch (e: SerializationException) {
        Result.failure(ApiException(ApiError.InvalidResponse))
    } catch (e: Exception) {
        Result.failure(ApiException(ApiError.Unknown(e.message)))
    }

    private suspend fun ResponseException.toApiError(): ApiError {
        val message = runCatching { response.body<ErrorDto>().error }.getOrNull()
        val status = response.status
        return when {
            status == HttpStatusCode.Unauthorized || status == HttpStatusCode.Forbidden -> ApiError.Unauthorized
            status == HttpStatusCode.NotFound -> ApiError.NotFound
            status == HttpStatusCode.TooManyRequests -> ApiError.RateLimited
            status.value >= 500 -> ApiError.Server(message)
            // Server has no dedicated status for sold-out; the message is the only signal.
            message?.contains("sold out", ignoreCase = true) == true -> ApiError.SoldOut(message)
            else -> ApiError.Unknown(message)
        }
    }

    private companion object {
        // Strict on purpose: java.util.UUID.fromString is lenient and accepts "1-1-1-1-1".
        val UuidRegex = Regex("^[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$")
    }
}
