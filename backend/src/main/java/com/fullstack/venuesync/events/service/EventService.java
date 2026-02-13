package com.fullstack.venuesync.events.service;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.fullstack.venuesync.events.domain.CreateEventRequest;
import com.fullstack.venuesync.events.domain.UpdateEventRequest;
import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.events.domain.EventStatusEnum;

public interface  EventService {

  Event createEvent(UUID organizerId, CreateEventRequest event);
  Page<Event> listEventsForOrganizer(UUID organizerId, Pageable pageable);
  Page<Event> listEventsForOrganizerByStatus(UUID organizerId, EventStatusEnum status, Pageable pageable);
  long countEventsForOrganizerByStatus(UUID organizerId, EventStatusEnum status);
  Optional<Event> getEventForOrganizer(UUID organizerId, UUID id);
  Event updateEventForOrganizer(UUID organizerId, UUID id, UpdateEventRequest event);
  void deleteEventForOrganizer(UUID organizerId, UUID id);
  Page<Event> listPublishedEvents(Pageable pageable);
  Page<Event> searchPublishedEvents(String query, Pageable pageable);
  Optional<Event> getPublishedEvent(UUID id);
}
