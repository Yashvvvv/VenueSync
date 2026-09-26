package com.venuesync.app.core.model

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Domain model — what the UI and ViewModel see. Never a wire DTO: the backend's field
 * names, string timestamps and error taxonomy stop at the repository boundary.
 */
data class Event(
    val id: String,
    val name: String,
    val start: LocalDateTime?,
    val end: LocalDateTime?,
    val venue: String?,
)

internal fun ListPublishedEventResponseDto.toDomain() = Event(
    id = id,
    name = name,
    start = start?.toLocalDateTimeOrNull(),
    end = end?.toLocalDateTimeOrNull(),
    venue = venue,
)

data class EventDetail(
    val id: String,
    val name: String,
    val start: LocalDateTime?,
    val end: LocalDateTime?,
    val venue: String?,
    val ticketTypes: List<TicketType>,
)

data class TicketType(
    val id: String,
    val name: String,
    val price: BigDecimal,
    val description: String?,
)

/**
 * Returns null when the server broke the contract (no id or name); the repository turns
 * that into [ApiError.InvalidResponse]. A single bad ticket type is dropped, not fatal.
 */
internal fun GetPublishedEventDetailsResponseDto.toDomainOrNull(): EventDetail? {
    val id = id?.takeIf { it.isNotBlank() } ?: return null
    val name = name?.takeIf { it.isNotBlank() } ?: return null
    return EventDetail(
        id = id,
        name = name,
        start = start?.toLocalDateTimeOrNull(),
        end = end?.toLocalDateTimeOrNull(),
        venue = venue?.takeIf { it.isNotBlank() },
        // distinctBy: LazyColumn crashes on duplicate keys, and ids are the keys.
        ticketTypes = ticketTypes.orEmpty().mapNotNull { it.toDomainOrNull() }.distinctBy { it.id },
    )
}

/** Nobody can buy a ticket with an unknown or impossible price, so those are dropped. */
internal fun PublishedTicketTypeDto.toDomainOrNull(): TicketType? {
    val id = id?.takeIf { it.isNotBlank() } ?: return null
    val name = name?.takeIf { it.isNotBlank() } ?: return null
    val price = price?.takeIf { it.isFinite() && it >= 0 } ?: return null
    return TicketType(
        id = id,
        name = name,
        price = BigDecimal.valueOf(price), // valueOf(19.99) == 19.99; BigDecimal(19.99) == 19.98999…
        description = description?.takeIf { it.isNotBlank() },
    )
}

private fun String.toLocalDateTimeOrNull(): LocalDateTime? =
    runCatching { LocalDateTime.parse(this) }.getOrNull()
