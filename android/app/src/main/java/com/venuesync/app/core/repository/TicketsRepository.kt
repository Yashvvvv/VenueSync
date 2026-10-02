package com.venuesync.app.core.repository

import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.Ticket
import com.venuesync.app.core.model.toDomainOrNull
import com.venuesync.app.core.network.TicketsApi
import javax.inject.Inject

interface TicketsRepository {
    /** Buys one ticket. Repeating with the same [idempotencyKey] returns the same ticket, never a second one. */
    suspend fun purchase(eventId: String, ticketTypeId: String, idempotencyKey: String): Result<Ticket>
}

/** Trust boundary for tickets: same rules as [EventsRepositoryImpl]. */
class TicketsRepositoryImpl @Inject constructor(
    private val api: TicketsApi,
) : TicketsRepository {

    override suspend fun purchase(eventId: String, ticketTypeId: String, idempotencyKey: String): Result<Ticket> {
        // Ids land in the URL path; anything that isn't a UUID never reaches the network.
        if (!UuidRegex.matches(eventId) || !UuidRegex.matches(ticketTypeId)) {
            return Result.failure(ApiException(ApiError.NotFound))
        }
        // The server would answer 400; failing here keeps a client bug from costing a round trip.
        if (!UuidRegex.matches(idempotencyKey)) {
            return Result.failure(ApiException(ApiError.Unknown("invalid idempotency key")))
        }
        return apiCall {
            api.purchase(eventId, ticketTypeId, idempotencyKey).toDomainOrNull()
                ?.takeIf { UuidRegex.matches(it.id) && it.eventId.equals(eventId, ignoreCase = true) }
                ?: throw ApiException(ApiError.InvalidResponse)
        }
    }
}
