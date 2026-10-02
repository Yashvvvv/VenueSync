package com.venuesync.app.core.model

import java.math.BigDecimal
import java.time.LocalDateTime

enum class TicketStatus { Purchased, Used, Expired, Cancelled, Unknown }

/** A ticket the user owns. Domain model: wire names and string timestamps stop at the repository. */
data class Ticket(
    val id: String,
    val status: TicketStatus,
    val ticketTypeName: String,
    /** Null when the server sent nothing usable; the UI then shows no price. */
    val price: BigDecimal?,
    val eventId: String,
    val eventName: String,
    val venue: String?,
    val eventStart: LocalDateTime?,
    val eventEnd: LocalDateTime?,
    val purchasedAt: LocalDateTime?,
)

/**
 * Returns null when the server broke the contract; the repository turns that into
 * [ApiError.InvalidResponse]. Id shape and "is this OUR event" are checked by the repository.
 */
internal fun TicketDto.toDomainOrNull(): Ticket? {
    val id = id?.takeIf { it.isNotBlank() } ?: return null
    val eventId = eventId?.takeIf { it.isNotBlank() } ?: return null
    val ticketTypeName = ticketTypeName?.takeIf { it.isNotBlank() } ?: return null
    val eventName = eventName?.takeIf { it.isNotBlank() } ?: return null
    return Ticket(
        id = id,
        status = status.toTicketStatus(),
        ticketTypeName = ticketTypeName,
        price = price?.takeIf { it.isFinite() && it >= 0 }?.let { BigDecimal.valueOf(it) },
        eventId = eventId,
        eventName = eventName,
        venue = eventVenue?.takeIf { it.isNotBlank() },
        eventStart = eventStart?.toLocalDateTimeOrNull(),
        eventEnd = eventEnd?.toLocalDateTimeOrNull(),
        purchasedAt = purchasedAt?.toLocalDateTimeOrNull(),
    )
}

internal fun String?.toTicketStatus(): TicketStatus = when (this) {
    "PURCHASED" -> TicketStatus.Purchased
    "USED" -> TicketStatus.Used
    "EXPIRED" -> TicketStatus.Expired
    "CANCELLED" -> TicketStatus.Cancelled
    else -> TicketStatus.Unknown
}

/** A row in My Tickets. Not [Ticket]: the list endpoint sends no eventId or venue. */
data class TicketSummary(
    val id: String,
    val status: TicketStatus,
    val ticketTypeName: String,
    val eventName: String,
    val eventStart: LocalDateTime?,
)

/** Server-side split: Active = purchased and the event hasn't ended; Past = everything else. */
enum class TicketFilter(val wire: String) { Active("active"), Past("past") }

/** Null when the row breaks the contract; the repository drops it and keeps the rest. */
internal fun ListTicketDto.toDomainOrNull(): TicketSummary? {
    val id = id?.takeIf { it.isNotBlank() } ?: return null
    val ticketTypeName = ticketType?.name?.takeIf { it.isNotBlank() } ?: return null
    val eventName = eventName?.takeIf { it.isNotBlank() } ?: return null
    return TicketSummary(
        id = id,
        status = status.toTicketStatus(),
        ticketTypeName = ticketTypeName,
        eventName = eventName,
        eventStart = eventStart?.toLocalDateTimeOrNull(),
    )
}
