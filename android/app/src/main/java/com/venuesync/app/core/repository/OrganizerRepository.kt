package com.venuesync.app.core.repository

import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.EventCounts
import com.venuesync.app.core.model.EventDraft
import com.venuesync.app.core.model.EventStatus
import com.venuesync.app.core.model.OrganizerEvent
import com.venuesync.app.core.model.OrganizerEventDto
import com.venuesync.app.core.model.StaffInvite
import com.venuesync.app.core.model.StaffMember
import com.venuesync.app.core.model.firstInvalidField
import com.venuesync.app.core.model.toDomainOrNull
import com.venuesync.app.core.model.toEventCounts
import com.venuesync.app.core.model.toLocalDateTimeOrNull
import com.venuesync.app.core.model.toWire
import com.venuesync.app.core.network.OrganizerApi
import javax.inject.Inject

/** A page of the organizer's events plus whether the server has more. */
data class OrganizerEventPage(val events: List<OrganizerEvent>, val isLast: Boolean)

/** An organizer's own events, door staff and the upgrade. Every failure is an [ApiException] in the [Result]. */
interface OrganizerRepository {
    /** [status] null = every status. */
    suspend fun events(status: EventStatus?, page: Int, size: Int = DEFAULT_PAGE_SIZE): Result<OrganizerEventPage>
    suspend fun counts(): Result<EventCounts>
    suspend fun event(id: String): Result<OrganizerEvent>
    /** [idempotencyKey]: one per create attempt; reuse it for a retry so a lost answer can't make two events. */
    suspend fun create(draft: EventDraft, idempotencyKey: String): Result<OrganizerEvent>
    suspend fun update(id: String, draft: EventDraft): Result<OrganizerEvent>
    suspend fun delete(id: String): Result<Unit>
    suspend fun staff(eventId: String): Result<List<StaffMember>>
    suspend fun createInvite(eventId: String): Result<StaffInvite>
    suspend fun removeStaff(eventId: String, userId: String): Result<Unit>
    suspend fun becomeOrganizer(): Result<Unit>
}

/**
 * The trust boundary for organizer data: a draft is checked before it leaves (so the form marks a mistake without a
 * round trip, and nothing malformed is sent), ids are checked before they reach a URL, and every answer is checked
 * before it reaches a screen.
 */
class OrganizerRepositoryImpl @Inject constructor(
    private val api: OrganizerApi,
) : OrganizerRepository {

    override suspend fun events(status: EventStatus?, page: Int, size: Int): Result<OrganizerEventPage> {
        if (status == EventStatus.Unknown || page < 0) return invalid(null)
        return apiCall {
            val response = api.listEvents(status?.wire, page, size.coerceIn(1, 50))
            // One malformed event is dropped from the list (it can still be opened, and then fails on its own), so
            // a single bad row never blanks the dashboard.
            OrganizerEventPage(response.content.mapNotNull { it.toDomainOrNull() }.distinctBy { it.id }, response.last)
        }
    }

    override suspend fun counts(): Result<EventCounts> = apiCall {
        api.counts().mapValues { (_, count) -> count.coerceAtLeast(0) }.toEventCounts()
    }

    override suspend fun event(id: String): Result<OrganizerEvent> {
        if (!UuidRegex.matches(id)) return Result.failure(ApiException(ApiError.NotFound))
        return apiCall { api.getEvent(id).toEvent(expectedId = id) }
    }

    override suspend fun create(draft: EventDraft, idempotencyKey: String): Result<OrganizerEvent> {
        draft.firstInvalidField(creating = true)?.let { return invalid(it) }
        if (!UuidRegex.matches(idempotencyKey)) return invalid(null) // a caller bug, never sent
        return apiCall { api.createEvent(draft.toWire(id = null), idempotencyKey).toEvent(expectedId = null) }
    }

    override suspend fun update(id: String, draft: EventDraft): Result<OrganizerEvent> {
        if (!UuidRegex.matches(id)) return Result.failure(ApiException(ApiError.NotFound))
        draft.firstInvalidField(creating = false)?.let { return invalid(it) }
        if (draft.ticketTypes.any { it.id != null && !UuidRegex.matches(it.id) }) return invalid("ticketTypes")
        return apiCall { api.updateEvent(id, draft.toWire(id = id)).toEvent(expectedId = id) }
    }

    override suspend fun delete(id: String): Result<Unit> {
        if (!UuidRegex.matches(id)) return Result.failure(ApiException(ApiError.NotFound))
        return apiCall { api.deleteEvent(id) }
    }

    override suspend fun staff(eventId: String): Result<List<StaffMember>> {
        if (!UuidRegex.matches(eventId)) return Result.failure(ApiException(ApiError.NotFound))
        return apiCall { api.staff(eventId).mapNotNull { it.toDomainOrNull() }.distinctBy { it.userId } }
    }

    override suspend fun createInvite(eventId: String): Result<StaffInvite> {
        if (!UuidRegex.matches(eventId)) return Result.failure(ApiException(ApiError.NotFound))
        return apiCall {
            val dto = api.createInvite(eventId)
            val code = dto.code?.takeIf { it.isNotBlank() } ?: throw ApiException(ApiError.InvalidResponse)
            StaffInvite(code, dto.expiresAt?.toLocalDateTimeOrNull())
        }
    }

    override suspend fun removeStaff(eventId: String, userId: String): Result<Unit> {
        if (!UuidRegex.matches(eventId) || !UuidRegex.matches(userId)) {
            return Result.failure(ApiException(ApiError.NotFound))
        }
        return apiCall { api.removeStaff(eventId, userId) }
    }

    override suspend fun becomeOrganizer(): Result<Unit> = apiCall { api.becomeOrganizer() }

    /** The server answered the question asked: a well-formed event, and the one in the URL when there is one. */
    private fun OrganizerEventDto.toEvent(expectedId: String?): OrganizerEvent =
        toDomainOrNull()?.takeIf { expectedId == null || it.id.equals(expectedId, ignoreCase = true) }
            ?: throw ApiException(ApiError.InvalidResponse)

    private fun <T> invalid(field: String?): Result<T> = Result.failure(ApiException(ApiError.Invalid(field)))
}
