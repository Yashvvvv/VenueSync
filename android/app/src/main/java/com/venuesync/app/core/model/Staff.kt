package com.venuesync.app.core.model

import kotlinx.serialization.Serializable

/** POST /staff-invites/{code}/accept */
@Serializable
data class AcceptStaffInviteResponseDto(
    val eventId: String? = null,
    val eventName: String? = null,
)

/** GET /staff/events/{eventId}/guests: one row. */
@Serializable
data class GuestDto(
    val ticketId: String? = null,
    val ticketCode: String? = null,
    val attendeeName: String? = null,
    val attendeeEmail: String? = null,
    val ticketTypeName: String? = null,
    val status: String? = null,
)

/** The event a redeemed invite put you on the door of. */
data class JoinedEvent(val eventId: String, val eventName: String)

/** A guest at the door. The email arrives masked (ya***@gmail.com). */
data class Guest(
    val ticketId: String,
    val ticketCode: String,
    val name: String?,
    val email: String?,
    val ticketTypeName: String?,
    val status: TicketStatus,
)

internal fun GuestDto.toDomainOrNull(): Guest? {
    val ticketId = ticketId?.takeIf { it.isNotBlank() } ?: return null
    return Guest(
        ticketId = ticketId,
        ticketCode = ticketCode?.takeIf { it.isNotBlank() } ?: ticketCodeOf(ticketId),
        name = attendeeName?.takeIf { it.isNotBlank() },
        email = attendeeEmail?.takeIf { it.isNotBlank() },
        ticketTypeName = ticketTypeName?.takeIf { it.isNotBlank() },
        status = status.toTicketStatus(),
    )
}

/**
 * Invite codes as people type them: K7Q2M-9XH4P, lower case, spaces, O for 0, I or L for 1. Null when it can't be
 * a code (10 Crockford base32 characters), so nothing is sent. Mirrors the server's StaffInviteCodes.
 */
fun normalizeInviteCode(input: String): String? {
    val code = input.uppercase().filterNot { it.isWhitespace() || it == '-' }
        .replace('O', '0').replace('I', '1').replace('L', '1')
    return code.takeIf { it.length == 10 && it.all { c -> c in InviteAlphabet } }
}

private const val InviteAlphabet = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
