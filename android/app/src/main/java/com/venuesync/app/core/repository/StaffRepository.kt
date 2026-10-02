package com.venuesync.app.core.repository

import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.Event
import com.venuesync.app.core.model.Guest
import com.venuesync.app.core.model.JoinedEvent
import com.venuesync.app.core.model.normalizeInviteCode
import com.venuesync.app.core.model.toDomain
import com.venuesync.app.core.model.toDomainOrNull
import com.venuesync.app.core.network.StaffApi
import javax.inject.Inject

interface StaffRepository {
    /** The events whose door this user can work. */
    suspend fun staffingEvents(): Result<List<Event>>

    /** Redeems an organizer's invite code; the user then staffs that event. */
    suspend fun acceptInvite(code: String): Result<JoinedEvent>

    /** The event's guests matching name, email or ticket code (at least 2 characters). */
    suspend fun searchGuests(eventId: String, query: String): Result<List<Guest>>
}

class StaffRepositoryImpl @Inject constructor(
    private val api: StaffApi,
) : StaffRepository {

    override suspend fun staffingEvents(): Result<List<Event>> = apiCall {
        api.staffingEvents().map { it.toDomain() }.distinctBy { it.id }
    }

    override suspend fun acceptInvite(code: String): Result<JoinedEvent> {
        // A malformed code can't exist on the server; it never reaches the network (and never lands in a URL raw).
        val normalized = normalizeInviteCode(code) ?: return Result.failure(ApiException(ApiError.NotFound))
        return apiCall {
            val dto = api.acceptInvite(normalized)
            val eventId = dto.eventId?.takeIf { UuidRegex.matches(it) } ?: throw ApiException(ApiError.InvalidResponse)
            JoinedEvent(eventId, dto.eventName?.takeIf { it.isNotBlank() } ?: "your event")
        }
    }

    override suspend fun searchGuests(eventId: String, query: String): Result<List<Guest>> {
        if (!UuidRegex.matches(eventId)) return Result.failure(ApiException(ApiError.NotFound))
        val q = query.trim()
        if (q.length < 2) return Result.success(emptyList()) // the server would answer the same, without a request
        return apiCall { api.guests(eventId, q).mapNotNull { it.toDomainOrNull() }.distinctBy { it.ticketId } }
    }
}
