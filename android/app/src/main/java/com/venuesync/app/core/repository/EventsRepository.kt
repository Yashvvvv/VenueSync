package com.venuesync.app.core.repository

import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.Event
import com.venuesync.app.core.model.EventDetail
import com.venuesync.app.core.model.toDomainOrNull
import com.venuesync.app.core.network.EventsApi
import javax.inject.Inject

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

    override suspend fun getPublishedEvents(query: String?, page: Int): Result<EventPage> = apiCall {
        val response = api.getPublishedEvents(query = query, page = page)
        EventPage(events = response.content.mapNotNull { it.toDomainOrNull() }, isLast = response.last)
    }

    override suspend fun getPublishedEvent(id: String): Result<EventDetail> {
        // Ids can come from deep links later; anything that isn't a UUID never reaches the network.
        if (!UuidRegex.matches(id)) return Result.failure(ApiException(ApiError.NotFound))
        return apiCall {
            api.getPublishedEvent(id).toDomainOrNull()
                ?.takeIf { it.id.equals(id, ignoreCase = true) } // server answered the question we asked
                ?: throw ApiException(ApiError.InvalidResponse)
        }
    }
}
