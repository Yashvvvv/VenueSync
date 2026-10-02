package com.fullstack.venuesync.staff.dto;

import java.util.UUID;

public record EventStaffMemberDto(UUID userId, String name, String email) {
}
