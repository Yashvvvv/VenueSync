package com.fullstack.venuesync.staff.service;

import com.fullstack.venuesync.events.repository.EventRepository;
import com.fullstack.venuesync.shared.domain.UserRepository;
import com.fullstack.venuesync.staff.exception.NotEventStaffException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Who may work an event's door. Being on the event's staff (or organizing it) IS the permission - there is no
 * global staff role, so a door team can never scan another organizer's tickets.
 */
@Service
@RequiredArgsConstructor
public class EventStaffService {

  private final EventRepository eventRepository;
  private final UserRepository userRepository;

  public boolean canScan(UUID userId, UUID eventId) {
    return eventRepository.existsByIdAndOrganizerId(eventId, userId) || userRepository.isStaffOf(userId, eventId);
  }

  public void requireCanScan(UUID userId, UUID eventId) {
    if (!canScan(userId, eventId)) {
      throw new NotEventStaffException(String.format("User %s is not staff of event %s", userId, eventId));
    }
  }
}
