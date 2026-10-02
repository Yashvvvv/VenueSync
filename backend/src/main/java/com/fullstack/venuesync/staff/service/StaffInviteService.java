package com.fullstack.venuesync.staff.service;

import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.events.exception.EventNotFoundException;
import com.fullstack.venuesync.events.repository.EventRepository;
import com.fullstack.venuesync.shared.domain.User;
import com.fullstack.venuesync.shared.domain.UserRepository;
import com.fullstack.venuesync.shared.exceptions.UserNotFoundException;
import com.fullstack.venuesync.staff.domain.StaffInvite;
import com.fullstack.venuesync.staff.dto.AcceptStaffInviteResponseDto;
import com.fullstack.venuesync.staff.dto.EventStaffMemberDto;
import com.fullstack.venuesync.staff.dto.StaffInviteResponseDto;
import com.fullstack.venuesync.staff.exception.StaffInviteExpiredException;
import com.fullstack.venuesync.staff.exception.StaffInviteNotFoundException;
import com.fullstack.venuesync.staff.exception.StaffInviteUsedException;
import com.fullstack.venuesync.staff.repository.StaffInviteRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Organizers build their door team; nobody needs a role assigned in Auth0. */
@Service
@RequiredArgsConstructor
@Transactional
public class StaffInviteService {

  static final Duration VALIDITY = Duration.ofDays(7);

  private final StaffInviteRepository inviteRepository;
  private final EventRepository eventRepository;
  private final UserRepository userRepository;

  public StaffInviteResponseDto createInvite(UUID organizerId, UUID eventId) {
    Event event = ownedEvent(organizerId, eventId);
    User organizer = userRepository.findById(organizerId)
        .orElseThrow(() -> new UserNotFoundException("User " + organizerId + " not found"));

    // ~50 bits: a clash is astronomically rare, but a retry costs nothing and the unique constraint backs it up.
    String code = StaffInviteCodes.generate();
    for (int attempt = 0; attempt < 5 && inviteRepository.existsByCode(code); attempt++) {
      code = StaffInviteCodes.generate();
    }

    StaffInvite invite = new StaffInvite();
    invite.setCode(code);
    invite.setEvent(event);
    invite.setCreatedBy(organizer);
    invite.setCreatedAt(LocalDateTime.now());
    invite.setExpiresAt(invite.getCreatedAt().plus(VALIDITY));
    inviteRepository.save(invite);
    return new StaffInviteResponseDto(StaffInviteCodes.format(code), invite.getExpiresAt());
  }

  public AcceptStaffInviteResponseDto acceptInvite(UUID userId, String rawCode) {
    String code = StaffInviteCodes.normalize(rawCode).orElseThrow(StaffInviteNotFoundException::new);
    StaffInvite invite = inviteRepository.findByCodeForUpdate(code).orElseThrow(StaffInviteNotFoundException::new);
    Event event = invite.getEvent();

    if (invite.getAcceptedBy() != null) {
      // Opening the same link twice is fine; using someone else's code is not.
      if (invite.getAcceptedBy().getId().equals(userId)) {
        return new AcceptStaffInviteResponseDto(event.getId(), event.getName());
      }
      throw new StaffInviteUsedException();
    }
    if (LocalDateTime.now().isAfter(invite.getExpiresAt())) {
      throw new StaffInviteExpiredException();
    }

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User " + userId + " not found"));
    if (user.getStaffingEvents().stream().noneMatch(e -> e.getId().equals(event.getId()))) {
      user.getStaffingEvents().add(event);
    }
    invite.setAcceptedBy(user);
    invite.setAcceptedAt(LocalDateTime.now());
    return new AcceptStaffInviteResponseDto(event.getId(), event.getName());
  }

  @Transactional(readOnly = true)
  public List<EventStaffMemberDto> listStaff(UUID organizerId, UUID eventId) {
    return ownedEvent(organizerId, eventId).getStaff().stream()
        .map(u -> new EventStaffMemberDto(u.getId(), u.getName(), u.getEmail()))
        .toList();
  }

  /** Idempotent: removing someone who isn't (or is no longer) staff succeeds too. */
  public void removeStaff(UUID organizerId, UUID eventId, UUID staffUserId) {
    ownedEvent(organizerId, eventId);
    userRepository.findById(staffUserId)
        .ifPresent(u -> u.getStaffingEvents().removeIf(e -> e.getId().equals(eventId)));
  }

  /** Another organizer's event is "not found", not "forbidden": event ids don't leak through this API. */
  private Event ownedEvent(UUID organizerId, UUID eventId) {
    return eventRepository.findByIdAndOrganizerId(eventId, organizerId)
        .orElseThrow(() -> new EventNotFoundException("Event " + eventId + " not found"));
  }
}
