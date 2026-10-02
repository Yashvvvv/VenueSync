package com.fullstack.venuesync.staff.dto;

import java.util.UUID;

/** The event the user now works. */
public record AcceptStaffInviteResponseDto(UUID eventId, String eventName) {
}
