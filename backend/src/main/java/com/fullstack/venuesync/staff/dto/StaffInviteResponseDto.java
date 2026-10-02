package com.fullstack.venuesync.staff.dto;

import java.time.LocalDateTime;

/** A freshly created invite: the code to hand over, displayed as XXXXX-XXXXX. */
public record StaffInviteResponseDto(String code, LocalDateTime expiresAt) {
}
