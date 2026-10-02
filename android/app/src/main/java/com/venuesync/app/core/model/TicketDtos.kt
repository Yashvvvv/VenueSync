package com.venuesync.app.core.model

import kotlinx.serialization.Serializable

/**
 * POST /events/{eventId}/ticket-types/{ticketTypeId}/tickets — backend GetTicketResponseDto.
 * Every field is optional on the wire; the mapper (toDomainOrNull) decides what is required.
 */
@Serializable
data class TicketDto(
    val id: String? = null,
    /** Short code for manual check-in, e.g. F5A3-038B. Older servers don't send it; see Ticket.code. */
    val ticketCode: String? = null,
    // String, not an enum: a new status value must not fail the whole response.
    val status: String? = null,
    val ticketTypeName: String? = null,
    val price: Double? = null,
    val description: String? = null,
    val eventId: String? = null,
    val eventName: String? = null,
    val eventVenue: String? = null,
    val eventStart: String? = null,
    val eventEnd: String? = null,
    val purchasedAt: String? = null,
)

/** GET /tickets — one row of Page<ListTicketResponseDto>. */
@Serializable
data class ListTicketDto(
    val id: String? = null,
    val status: String? = null,
    val ticketType: ListTicketTypeDto? = null,
    val eventName: String? = null,
    val eventStart: String? = null,
    val eventEnd: String? = null,
)

@Serializable
data class ListTicketTypeDto(
    val id: String? = null,
    val name: String? = null,
    val price: Double? = null,
)
