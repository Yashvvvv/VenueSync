package com.fullstack.venuesync.staff.dto;

import com.fullstack.venuesync.tickets.domain.TicketStatusEnum;
import java.util.UUID;

/** One row of the door guest list. The email is masked: staff compare a name against an ID, not an address. */
public record GuestDto(
    UUID ticketId,
    String ticketCode,
    String attendeeName,
    String attendeeEmail,
    String ticketTypeName,
    TicketStatusEnum status) {
}
