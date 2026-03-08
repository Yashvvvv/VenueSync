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

  /**
   * Creates a new event for the specified organizer.
   *
   * @param organizerId the UUID of the organizer creating the event
   * @param event the event creation request containing event details and ticket types
   * @return the newly created Event entity
   * @throws com.fullstack.venuesync.shared.exceptions.UserNotFoundException if the organizer is not found
   */
  Event createEvent(UUID organizerId, CreateEventRequest event);

  /**
   * Lists all events belonging to the specified organizer with pagination.
   *
   * @param organizerId the UUID of the organizer
   * @param pageable pagination parameters
   * @return a paginated list of events
   */
  Page<Event> listEventsForOrganizer(UUID organizerId, Pageable pageable);

  /**
   * Lists events for an organizer filtered by status with pagination.
   *
   * @param organizerId the UUID of the organizer
   * @param status the event status to filter by
   * @param pageable pagination parameters
   * @return a paginated list of events matching the status
   */
  Page<Event> listEventsForOrganizerByStatus(UUID organizerId, EventStatusEnum status, Pageable pageable);

  /**
   * Counts the number of events for an organizer filtered by status.
   *
   * @param organizerId the UUID of the organizer
   * @param status the event status to count
   * @return the count of events with the specified status
   */
  long countEventsForOrganizerByStatus(UUID organizerId, EventStatusEnum status);

  /**
   * Retrieves a specific event belonging to an organizer.
   *
   * @param organizerId the UUID of the organizer
   * @param id the UUID of the event
   * @return an Optional containing the event if found and owned by the organizer
   */
  Optional<Event> getEventForOrganizer(UUID organizerId, UUID id);

  /**
   * Updates an existing event for the specified organizer.
   * Handles ticket type creation, update, and deletion.
   *
   * @param organizerId the UUID of the organizer
   * @param id the UUID of the event to update
   * @param event the update request containing new event details
   * @return the updated Event entity
   * @throws com.fullstack.venuesync.events.exception.EventNotFoundException if the event is not found
   * @throws com.fullstack.venuesync.events.exception.EventUpdateException if the event ID is null or mismatched
   */
  Event updateEventForOrganizer(UUID organizerId, UUID id, UpdateEventRequest event);

  /**
   * Deletes an event belonging to the specified organizer.
   *
   * @param organizerId the UUID of the organizer
   * @param id the UUID of the event to delete
   */
  void deleteEventForOrganizer(UUID organizerId, UUID id);

  /**
   * Lists all published events with pagination (public, no auth required).
   *
   * @param pageable pagination parameters
   * @return a paginated list of published events
   */
  Page<Event> listPublishedEvents(Pageable pageable);

  /**
   * Searches published events by name or venue with pagination.
   *
   * @param query the search query string
   * @param pageable pagination parameters
   * @return a paginated list of matching published events
   */
  Page<Event> searchPublishedEvents(String query, Pageable pageable);

  /**
   * Retrieves a specific published event by ID.
   *
   * @param id the UUID of the published event
   * @return an Optional containing the event if found and published
   */
  Optional<Event> getPublishedEvent(UUID id);
}
