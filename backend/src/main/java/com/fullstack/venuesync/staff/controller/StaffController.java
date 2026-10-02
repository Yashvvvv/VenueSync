package com.fullstack.venuesync.staff.controller;

import static com.fullstack.venuesync.shared.security.JwtUtil.parseUserId;

import com.fullstack.venuesync.events.domain.EventStatusEnum;
import com.fullstack.venuesync.events.dto.ListPublishedEventResponseDto;
import com.fullstack.venuesync.events.mapper.EventMapper;
import com.fullstack.venuesync.events.repository.EventRepository;
import com.fullstack.venuesync.staff.dto.AcceptStaffInviteResponseDto;
import com.fullstack.venuesync.staff.dto.EventStaffMemberDto;
import com.fullstack.venuesync.staff.dto.GuestDto;
import com.fullstack.venuesync.staff.dto.StaffInviteResponseDto;
import com.fullstack.venuesync.staff.service.GuestListService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StaffController {

  private final StaffInviteService staffInviteService;
  private final EventRepository eventRepository;
  private final EventMapper eventMapper;
  private final GuestListService guestListService;

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

  /** The events this user can scan (organized + staffed, published): the scanner's event picker. */
  @GetMapping("/users/me/staffing-events")
  public List<ListPublishedEventResponseDto> myStaffingEvents(@AuthenticationPrincipal Jwt jwt) {
    return eventRepository.findScannableBy(parseUserId(jwt), EventStatusEnum.PUBLISHED).stream()
        .map(eventMapper::toListPublishedEventResponseDto)
        .toList();
  }

  /** Staff or organizer of the event: search its guests (min. 2 characters, max. 20 results). */
  @GetMapping("/staff/events/{eventId}/guests")
  public List<GuestDto> searchGuests(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId, @RequestParam(defaultValue = "") String q) {
    return guestListService.search(parseUserId(jwt), eventId, q);
  }

  /** Any signed-in user: the code itself is the authorization. */
  @PostMapping("/staff-invites/{code}/accept")
  public AcceptStaffInviteResponseDto acceptInvite(@AuthenticationPrincipal Jwt jwt, @PathVariable String code) {
    return staffInviteService.acceptInvite(parseUserId(jwt), code);
  }
}
