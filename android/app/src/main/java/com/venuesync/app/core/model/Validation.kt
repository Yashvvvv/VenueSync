package com.venuesync.app.core.model

import kotlinx.serialization.Serializable

/** POST /ticket-validations body. `id` is the scanned QR code's UUID (not the ticket id). */
@Serializable
data class ValidationRequestDto(
    val id: String,
    // No default value: kotlinx.serialization skips defaults when encoding, and the server requires `method`.
    val method: String,
    /** The event this scanner admits for: a ticket for another event comes back WRONG_EVENT, untouched. */
    val eventId: String,
)

@Serializable
data class ValidationResponseDto(
    val ticketId: String? = null,
    // String, not an enum: a new status must show "couldn't verify", not fail the scan.
    val status: String? = null,
    val eventName: String? = null,
    val ticketTypeName: String? = null,
)

enum class ScanStatus { Valid, AlreadyUsed, Expired, Invalid, WrongEvent, Unknown }

/** What the door sees. Names are null when the server doesn't know the ticket. */
data class ScanResult(
    val status: ScanStatus,
    val ticketTypeName: String? = null,
    val eventName: String? = null,
)

/** Null when the server broke the contract (no status at all); the repository turns that into InvalidResponse. */
internal fun ValidationResponseDto.toDomainOrNull(): ScanResult? {
    val status = when (status ?: return null) {
        "VALID" -> ScanStatus.Valid
        "ALREADY_USED" -> ScanStatus.AlreadyUsed
        "EXPIRED" -> ScanStatus.Expired
        "INVALID" -> ScanStatus.Invalid
        "WRONG_EVENT" -> ScanStatus.WrongEvent
        else -> ScanStatus.Unknown
    }
    return ScanResult(
        status = status,
        ticketTypeName = ticketTypeName?.takeIf { it.isNotBlank() },
        eventName = eventName?.takeIf { it.isNotBlank() },
    )
}
