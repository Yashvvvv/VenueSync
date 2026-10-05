package com.venuesync.app.core.model

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime

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
    /** The event's own photo (see [toImagePathOrNull]); null shows the stand-in. */
    val imageUrl: String? = null,
)

/**
 * Only our own photo path is trusted: an image URL is loaded without asking, so anything that isn't
 * /api/v1/event-images/... (another host, a scheme, junk) is dropped and the stand-in shows instead.
 */
internal fun String?.toImagePathOrNull(): String? =
    this?.takeIf { it.length <= 200 && ImagePath.matches(it) }

private val ImagePath = Regex("^/api/v1/event-images/[0-9a-fA-F-]{36}(\\?v=\\d+)?$")

/** Null when the row has no id or name; the repository drops it and keeps the rest of the page. */
internal fun ListPublishedEventResponseDto.toDomainOrNull(): Event? {
    val id = id?.takeIf { it.isNotBlank() } ?: return null
    val name = name?.takeIf { it.isNotBlank() } ?: return null
    return Event(
        id = id,
        name = name,
        start = start?.toLocalDateTimeOrNull(),
        end = end?.toLocalDateTimeOrNull(),
        venue = venue?.takeIf { it.isNotBlank() },
        imageUrl = imageUrl.toImagePathOrNull(),
    )
}

data class EventDetail(
    val id: String,
    val name: String,
    val start: LocalDateTime?,
    val end: LocalDateTime?,
    val venue: String?,
    val ticketTypes: List<TicketType>,
    val salesStatus: SalesStatus,
    val salesStart: LocalDateTime?,
    val salesEnd: LocalDateTime?,
    val imageUrl: String? = null,
)

data class TicketType(
    val id: String,
    val name: String,
    val price: BigDecimal,
    val description: String?,
    val soldOut: Boolean,
)

/** Server-computed, never judged on the device clock. Unknown (missing or new value) lets the server decide. */
enum class SalesStatus { Upcoming, OnSale, Ended, Unknown }

internal fun String?.toSalesStatus(): SalesStatus = when (this) {
    "UPCOMING" -> SalesStatus.Upcoming
    "ON_SALE" -> SalesStatus.OnSale
    "ENDED" -> SalesStatus.Ended
    else -> SalesStatus.Unknown
}

/** What a ticket row offers. */
sealed interface Availability {
    data object Buyable : Availability
    data object SoldOut : Availability
    data class OnSaleFrom(val start: LocalDateTime?) : Availability
    data object SalesEnded : Availability
}

/** Unknown sales status stays Buyable: a wrong "can't buy" is worse than a purchase the server refuses. */
fun EventDetail.availabilityOf(type: TicketType): Availability = when {
    salesStatus == SalesStatus.Ended -> Availability.SalesEnded
    salesStatus == SalesStatus.Upcoming -> Availability.OnSaleFrom(salesStart)
    type.soldOut -> Availability.SoldOut
    else -> Availability.Buyable
}

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
        salesStatus = salesStatus.toSalesStatus(),
        salesStart = salesStart?.toLocalDateTimeOrNull(),
        salesEnd = salesEnd?.toLocalDateTimeOrNull(),
        imageUrl = imageUrl.toImagePathOrNull(),
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
        soldOut = soldOut == true, // missing → not sold out: the server is the authority at purchase time
    )
}

/**
 * Server times are wall-clock times in the event's zone, sent with that zone's offset
 * ("2026-10-05T19:00:00+05:30", ADR-003). Keep the wall clock exactly as the organizer entered it —
 * never convert to the device's zone. Servers before that change sent no offset; both parse.
 */
internal fun String.toLocalDateTimeOrNull(): LocalDateTime? =
    runCatching { OffsetDateTime.parse(this).toLocalDateTime() }
        .recoverCatching { LocalDateTime.parse(this) }
        .getOrNull()
