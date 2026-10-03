package com.venuesync.app.core.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

enum class TicketStatus { Purchased, Used, Expired, Cancelled, Unknown }

/** A ticket the user owns. Domain model: wire names and string timestamps stop at the repository. */
data class Ticket(
    val id: String,
    /** What door staff type when the QR code won't scan, e.g. F5A3-038B. */
    val code: String,
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
    /** Null when this is live data; when the network failed, the moment the copy on the phone was saved. */
    val savedAt: Instant? = null,
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
        code = ticketCode?.takeIf { it.isNotBlank() } ?: ticketCodeOf(id),
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
    /** Null when the server sent nothing usable; the row then shows no price. */
    val price: BigDecimal? = null,
    val eventEnd: LocalDateTime? = null,
) {
    /**
     * What the row says. The server keeps a ticket PURCHASED after its event ends, so, like the web, an ended event
     * reads as Expired. Display only: the scanner, not this, decides who gets in.
     */
    fun displayStatus(now: LocalDateTime): TicketStatus =
        if (status == TicketStatus.Purchased && eventEnd?.isBefore(now) == true) TicketStatus.Expired else status
}

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
        price = ticketType.price?.takeIf { it.isFinite() && it >= 0 }?.let { BigDecimal.valueOf(it) },
        eventEnd = eventEnd?.toLocalDateTimeOrNull(),
    )
}

/**
 * The server's rule (first 8 hex characters of the id, as XXXX-XXXX), used only when an older server sends no
 * ticketCode, so the code is never blank.
 */
internal fun ticketCodeOf(ticketId: String): String {
    val hex = ticketId.replace("-", "").take(8).uppercase()
    return if (hex.length == 8) "${hex.take(4)}-${hex.drop(4)}" else hex
}

/**
 * What staff typed, ready to send: a full ticket id as-is, or an 8-hex ticket code without its dash.
 * Null when it can't be either, so nothing is sent.
 */
fun normalizeCheckInEntry(entry: String): String? {
    val trimmed = entry.trim()
    if (UuidPattern.matches(trimmed)) return trimmed
    val hex = trimmed.replace("-", "").replace(" ", "")
    return hex.takeIf { it.length == 8 && it.all { c -> c.isDigit() || c.lowercaseChar() in 'a'..'f' } }
}

private val UuidPattern = Regex("^[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$")
