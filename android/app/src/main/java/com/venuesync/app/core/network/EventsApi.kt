package com.venuesync.app.core.network

import com.venuesync.app.core.model.ListPublishedEventResponseDto
import com.venuesync.app.core.model.PageResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText

/*
 * Thin endpoint wrapper — mechanical HTTP only, no error mapping (that's repository work).
 * Endpoints per client-api.md; base URL already carries /api/v1.
 */
class EventsApi(private val client: HttpClient) {

    /** M0 smoke test: raw JSON for logging. Delete with the MainActivity smoke block. */
    suspend fun publishedEventsRawJson(): String =
        client.get("published-events").bodyAsText()

    suspend fun getPublishedEvents(
        query: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): PageResponse<ListPublishedEventResponseDto> =
        client.get("published-events") {
            if (!query.isNullOrBlank()) parameter("q", query)
            parameter("page", page)
            parameter("size", size)
        }.body()
}
