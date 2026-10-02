package com.fullstack.venuesync.staff.controller;

import static com.fullstack.venuesync.shared.security.JwtUtil.parseUserId;

import com.fullstack.venuesync.staff.dto.AcceptStaffInviteResponseDto;
import com.fullstack.venuesync.staff.dto.EventStaffMemberDto;
import com.fullstack.venuesync.staff.dto.StaffInviteResponseDto;
import com.fullstack.venuesync.staff.service.StaffInviteService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StaffController {

  private final StaffInviteService staffInviteService;

  @PostMapping("/events/{eventId}/staff-invites")
  public ResponseEntity<StaffInviteResponseDto> createInvite(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId) {
    return ResponseEntity.status(HttpStatus.CREATED).body(staffInviteService.createInvite(parseUserId(jwt), eventId));
  }

  @GetMapping("/events/{eventId}/staff")
  public List<EventStaffMemberDto> listStaff(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId) {
    return staffInviteService.listStaff(parseUserId(jwt), eventId);
  }

  @DeleteMapping("/events/{eventId}/staff/{userId}")
  public ResponseEntity<Void> removeStaff(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId, @PathVariable UUID userId) {
    staffInviteService.removeStaff(parseUserId(jwt), eventId, userId);
    return ResponseEntity.noContent().build();
  }

  /** Any signed-in user: the code itself is the authorization. */
  @PostMapping("/staff-invites/{code}/accept")
  public AcceptStaffInviteResponseDto acceptInvite(@AuthenticationPrincipal Jwt jwt, @PathVariable String code) {
    return staffInviteService.acceptInvite(parseUserId(jwt), code);
  }
}
