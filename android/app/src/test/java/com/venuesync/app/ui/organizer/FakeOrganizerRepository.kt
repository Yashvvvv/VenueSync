package com.venuesync.app.ui.organizer

import com.venuesync.app.core.model.EventCounts
import com.venuesync.app.core.model.EventDraft
import com.venuesync.app.core.model.EventStatus
import com.venuesync.app.core.model.OrganizerEvent
import com.venuesync.app.core.model.OrganizerTicketType
import com.venuesync.app.core.model.StaffInvite
import com.venuesync.app.core.model.StaffMember
import com.venuesync.app.core.repository.OrganizerEventPage
import com.venuesync.app.core.repository.OrganizerRepository
import java.math.BigDecimal
import java.time.LocalDateTime

/** Answers from the vars below and records what was sent. Anything a test doesn't set up fails loudly. */
open class FakeOrganizerRepository : OrganizerRepository {
    var event: Result<OrganizerEvent>? = null
    var update: ((String, EventDraft) -> Result<OrganizerEvent>)? = null
    var create: ((EventDraft, String) -> Result<OrganizerEvent>)? = null
    var delete: Result<Unit>? = null
    var staff: Result<List<StaffMember>>? = null
    var invite: Result<StaffInvite>? = null
    var removeStaff: Result<Unit>? = null
    var becomeOrganizer: Result<Unit>? = null
    val removed = mutableListOf<String>()

    val updates = mutableListOf<Pair<String, EventDraft>>()
    val creates = mutableListOf<Pair<EventDraft, String>>()
    var eventCalls = 0

    override suspend fun events(status: EventStatus?, page: Int, size: Int): Result<OrganizerEventPage> = error("not used")
    override suspend fun counts(): Result<EventCounts> = error("not used")
    override suspend fun event(id: String): Result<OrganizerEvent> {
        eventCalls++
        return event ?: error("event not set up")
    }
    override suspend fun create(draft: EventDraft, idempotencyKey: String): Result<OrganizerEvent> {
        creates += draft to idempotencyKey
        return (create ?: error("create not set up"))(draft, idempotencyKey)
    }
    override suspend fun update(id: String, draft: EventDraft): Result<OrganizerEvent> {
        updates += id to draft
        return (update ?: error("update not set up"))(id, draft)
    }
    override suspend fun delete(id: String): Result<Unit> = delete ?: error("delete not set up")
    override suspend fun staff(eventId: String): Result<List<StaffMember>> = staff ?: error("staff not set up")
    override suspend fun createInvite(eventId: String): Result<StaffInvite> = invite ?: error("invite not set up")
    override suspend fun removeStaff(eventId: String, userId: String): Result<Unit> {
        removed += userId
        return removeStaff ?: error("removeStaff not set up")
    }
    override suspend fun becomeOrganizer(): Result<Unit> = becomeOrganizer ?: error("becomeOrganizer not set up")
}

const val EVENT_ID = "6dd8477a-c595-42ae-975a-940a56e2cf09"
const val TYPE_ID = "0c8f9522-aa34-414d-9f7a-85e330229f70"

fun organizerEvent(
    status: EventStatus = EventStatus.Draft,
    sold: Long = 0,
    capacity: Int? = 100,
    start: LocalDateTime? = LocalDateTime.of(2026, 11, 14, 19, 0),
    version: Long? = 3,
) = OrganizerEvent(
    id = EVENT_ID,
    name = "Night Market",
    start = start,
    end = start?.plusHours(4),
    venue = "Phoenix Hall, Pune",
    salesStart = null,
    salesEnd = null,
    status = status,
    ticketTypes = listOf(OrganizerTicketType(TYPE_ID, "General", BigDecimal("25.00"), null, capacity, sold)),
    version = version,
)
