package com.venuesync.app.core.model

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.Serializable

// ── Wire: the organizer endpoints (/events, /events/{id}/staff). Everything optional on the way in; the mappers
// decide what is required.

/** GET /events (a page) and GET /events/{id}: an organizer's own event. */
@Serializable
data class OrganizerEventDto(
    val id: String? = null,
    val name: String? = null,
    val start: String? = null,
    val end: String? = null,
    val venue: String? = null,
    val salesStart: String? = null,
    val salesEnd: String? = null,
    // String, not an enum: a status this app doesn't know must not fail the whole page.
    val status: String? = null,
    val ticketTypes: List<OrganizerTicketTypeDto>? = null,
)

@Serializable
data class OrganizerTicketTypeDto(
    val id: String? = null,
    val name: String? = null,
    val price: Double? = null,
    val description: String? = null,
    /** Null = unlimited. */
    val totalAvailable: Int? = null,
    /** Tickets issued so far (every status). Servers before 9.0 don't send it. */
    val sold: Long? = null,
)

/** POST /events and PUT /events/{id}. [id] only on update (the server checks it matches the path). */
@Serializable
data class EventWriteDto(
    val id: String? = null,
    val name: String,
    val start: String? = null,
    val end: String? = null,
    val venue: String,
    val salesStart: String? = null,
    val salesEnd: String? = null,
    val status: String,
    val ticketTypes: List<TicketTypeWriteDto>,
)

@Serializable
data class TicketTypeWriteDto(
    /** Null = a new ticket type; an existing id updates it. A type left out of an update is removed. */
    val id: String? = null,
    val name: String,
    val price: Double,
    val description: String? = null,
    val totalAvailable: Int? = null,
)

@Serializable
data class StaffMemberDto(val userId: String? = null, val name: String? = null, val email: String? = null)

@Serializable
data class StaffInviteDto(val code: String? = null, val expiresAt: String? = null)

// ── Domain.

enum class EventStatus(val wire: String) {
    Draft("DRAFT"), Published("PUBLISHED"), Cancelled("CANCELLED"), Completed("COMPLETED"),

    /** A status this app version doesn't know. Shown, never sent back. */
    Unknown("");

    companion object {
        fun of(wire: String?): EventStatus = entries.firstOrNull { it != Unknown && it.wire == wire } ?: Unknown
    }
}

data class OrganizerTicketType(
    val id: String,
    val name: String,
    val price: BigDecimal,
    val description: String?,
    /** Null = unlimited. */
    val capacity: Int?,
    val sold: Long,
)

data class OrganizerEvent(
    val id: String,
    val name: String,
    val start: LocalDateTime?,
    val end: LocalDateTime?,
    val venue: String,
    val salesStart: LocalDateTime?,
    val salesEnd: LocalDateTime?,
    val status: EventStatus,
    val ticketTypes: List<OrganizerTicketType>,
) {
    val sold: Long get() = ticketTypes.sumOf { it.sold }

    /** Revenue at each type's current price: an estimate once a price changes, since tickets keep their own. */
    val grossAtCurrentPrices: BigDecimal get() = ticketTypes.fold(BigDecimal.ZERO) { sum, type ->
        sum + type.price * BigDecimal.valueOf(type.sold)
    }

    fun toDraft() = EventDraft(
        name = name, venue = venue, start = start, end = end, salesStart = salesStart, salesEnd = salesEnd,
        status = status,
        ticketTypes = ticketTypes.map { TicketTypeDraft(it.id, it.name, it.price, it.description, it.capacity, it.sold) },
    )
}

/** How many of the organizer's events are in each status (GET /events/counts). */
data class EventCounts(val draft: Long, val published: Long, val cancelled: Long, val completed: Long)

/** What the create/edit form edits. Kept separate from [OrganizerEvent] so a half-typed form is a valid value. */
data class EventDraft(
    val name: String = "",
    val venue: String = "",
    val start: LocalDateTime? = null,
    val end: LocalDateTime? = null,
    val salesStart: LocalDateTime? = null,
    val salesEnd: LocalDateTime? = null,
    val status: EventStatus = EventStatus.Draft,
    val ticketTypes: List<TicketTypeDraft> = emptyList(),
)

data class TicketTypeDraft(
    /** Null until the server has it. */
    val id: String? = null,
    val name: String = "",
    val price: BigDecimal = BigDecimal.ZERO,
    val description: String? = null,
    val capacity: Int? = null,
    /** Tickets already issued for this type (0 for a new one): the form's floor for [capacity]. */
    val sold: Long = 0,
)

data class StaffMember(val userId: String, val name: String, val email: String?)

data class StaffInvite(val code: String, val expiresAt: LocalDateTime?)

// ── Mappers.

/**
 * Null when the server broke the contract. Unlike the public catalogue, a malformed ticket type fails the whole event
 * instead of being dropped: an edit sends the full list back, and the server removes any type left out of it.
 */
internal fun OrganizerEventDto.toDomainOrNull(): OrganizerEvent? {
    val id = id?.takeIf { it.isNotBlank() } ?: return null
    val name = name?.takeIf { it.isNotBlank() } ?: return null
    val types = ticketTypes.orEmpty().map { it.toDomainOrNull() ?: return null }
    if (types.map { it.id }.toSet().size != types.size) return null
    return OrganizerEvent(
        id = id,
        name = name,
        start = start?.toLocalDateTimeOrNull(),
        end = end?.toLocalDateTimeOrNull(),
        venue = venue.orEmpty(),
        salesStart = salesStart?.toLocalDateTimeOrNull(),
        salesEnd = salesEnd?.toLocalDateTimeOrNull(),
        status = EventStatus.of(status),
        ticketTypes = types,
    )
}

internal fun OrganizerTicketTypeDto.toDomainOrNull(): OrganizerTicketType? {
    val id = id?.takeIf { it.isNotBlank() } ?: return null
    val name = name?.takeIf { it.isNotBlank() } ?: return null
    val price = price?.takeIf { it.isFinite() && it >= 0 } ?: return null
    return OrganizerTicketType(
        id = id,
        name = name,
        price = BigDecimal.valueOf(price),
        description = description?.takeIf { it.isNotBlank() },
        capacity = totalAvailable?.takeIf { it >= 0 },
        sold = (sold ?: 0).coerceAtLeast(0),
    )
}

internal fun StaffMemberDto.toDomainOrNull(): StaffMember? {
    val userId = userId?.takeIf { it.isNotBlank() } ?: return null
    return StaffMember(userId, name?.takeIf { it.isNotBlank() } ?: "Unnamed", email?.takeIf { it.isNotBlank() })
}

internal fun Map<String, Long>.toEventCounts() = EventCounts(
    draft = this["draft"] ?: 0, published = this["published"] ?: 0,
    cancelled = this["cancelled"] ?: 0, completed = this["completed"] ?: 0,
)

/**
 * Wall clock, no offset (ADR-003): the backend reads a plain local date-time and the event happens at the time the
 * organizer typed, in the event's own zone.
 */
private val WireDateTime: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

internal fun EventDraft.toWire(id: String?) = EventWriteDto(
    id = id,
    name = name.trim(),
    start = start?.format(WireDateTime),
    end = end?.format(WireDateTime),
    venue = venue.trim(),
    salesStart = salesStart?.format(WireDateTime),
    salesEnd = salesEnd?.format(WireDateTime),
    status = status.wire,
    ticketTypes = ticketTypes.map {
        TicketTypeWriteDto(it.id, it.name.trim(), it.price.toDouble(), it.description?.trim()?.ifBlank { null }, it.capacity)
    },
)

/**
 * The first field that would be refused, checked on the phone so a mistake is marked before a round trip. Mirrors the
 * server's rules (bean validation and the 9.0 schedule checks); the server still has the last word.
 */
fun EventDraft.firstInvalidField(creating: Boolean): String? {
    if (name.trim().length !in 2..200) return "name"
    if (venue.trim().length !in 2..500) return "venue"
    if (creating && status != EventStatus.Draft && status != EventStatus.Published) return "status"
    if (status == EventStatus.Unknown) return "status"
    if (start != null && end != null && !end.isAfter(start)) return "end"
    if (salesStart != null && salesEnd != null && !salesEnd.isAfter(salesStart)) return "salesEnd"
    if (salesEnd != null && end != null && salesEnd.isAfter(end)) return "salesEnd"
    if (ticketTypes.isEmpty()) return "ticketTypes"
    ticketTypes.forEachIndexed { i, type ->
        if (type.name.trim().length !in 1..100) return "ticketTypes[$i].name"
        if (type.price.signum() < 0) return "ticketTypes[$i].price"
        if ((type.description?.trim()?.length ?: 0) > 500) return "ticketTypes[$i].description"
        if (type.capacity != null && (type.capacity < 0 || type.capacity < type.sold)) return "ticketTypes[$i].capacity"
    }
    return null
}
