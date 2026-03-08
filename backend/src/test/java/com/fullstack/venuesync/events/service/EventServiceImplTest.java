package com.fullstack.venuesync.events.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.fullstack.venuesync.events.domain.CreateEventRequest;
import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.events.domain.EventStatusEnum;
import com.fullstack.venuesync.events.domain.UpdateEventRequest;
import com.fullstack.venuesync.events.exception.EventNotFoundException;
import com.fullstack.venuesync.events.exception.EventUpdateException;
import com.fullstack.venuesync.events.repository.EventRepository;
import com.fullstack.venuesync.shared.domain.User;
import com.fullstack.venuesync.shared.domain.UserRepository;
import com.fullstack.venuesync.shared.exceptions.UserNotFoundException;
import com.fullstack.venuesync.tickets.domain.CreateTicketTypeRequest;
import com.fullstack.venuesync.tickets.domain.TicketType;
import com.fullstack.venuesync.tickets.domain.UpdateTicketTypeRequest;
import com.fullstack.venuesync.tickets.exception.TicketTypeNotFoundException;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private EventRepository eventRepository;

  @InjectMocks
  private EventServiceImpl eventService;

  private UUID organizerId;
  private UUID eventId;
  private User organizer;
  private Event event;

  @BeforeEach
  void setUp() {
    organizerId = UUID.randomUUID();
    eventId = UUID.randomUUID();

    organizer = new User();
    organizer.setId(organizerId);
    organizer.setName("Test Organizer");
    organizer.setEmail("organizer@test.com");

    event = new Event();
    event.setId(eventId);
    event.setName("Test Event");
    event.setVenue("Test Venue");
    event.setStatus(EventStatusEnum.DRAFT);
    event.setOrganizer(organizer);
    event.setTicketTypes(new ArrayList<>());
  }

  @Nested
  @DisplayName("createEvent")
  class CreateEventTests {

    @Test
    @DisplayName("should create event successfully")
    void shouldCreateEventSuccessfully() {
      CreateTicketTypeRequest ticketTypeReq = new CreateTicketTypeRequest("VIP", 100.0, "VIP Access", 50);
      CreateEventRequest request = new CreateEventRequest();
      request.setName("New Event");
      request.setVenue("Big Arena");
      request.setStatus(EventStatusEnum.DRAFT);
      request.setStart(LocalDateTime.now().plusDays(10));
      request.setEnd(LocalDateTime.now().plusDays(11));
      request.setTicketTypes(List.of(ticketTypeReq));

      when(userRepository.findById(organizerId)).thenReturn(Optional.of(organizer));
      when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
        Event saved = invocation.getArgument(0);
        saved.setId(UUID.randomUUID());
        return saved;
      });

      Event result = eventService.createEvent(organizerId, request);

      assertNotNull(result);
      assertEquals("New Event", result.getName());
      assertEquals("Big Arena", result.getVenue());
      assertEquals(EventStatusEnum.DRAFT, result.getStatus());
      assertEquals(organizer, result.getOrganizer());
      assertEquals(1, result.getTicketTypes().size());
      assertEquals("VIP", result.getTicketTypes().get(0).getName());

      verify(eventRepository).save(any(Event.class));
    }

    @Test
    @DisplayName("should throw UserNotFoundException when organizer not found")
    void shouldThrowUserNotFoundExceptionWhenOrganizerNotFound() {
      CreateEventRequest request = new CreateEventRequest();
      request.setName("New Event");
      request.setTicketTypes(List.of());

      when(userRepository.findById(organizerId)).thenReturn(Optional.empty());

      assertThrows(UserNotFoundException.class,
          () -> eventService.createEvent(organizerId, request));
    }
  }

  @Nested
  @DisplayName("listEventsForOrganizer")
  class ListEventsTests {

    @Test
    @DisplayName("should list events for organizer")
    void shouldListEventsForOrganizer() {
      Pageable pageable = PageRequest.of(0, 10);
      Page<Event> expectedPage = new PageImpl<>(List.of(event));

      when(eventRepository.findByOrganizerId(organizerId, pageable)).thenReturn(expectedPage);

      Page<Event> result = eventService.listEventsForOrganizer(organizerId, pageable);

      assertEquals(1, result.getTotalElements());
      assertEquals(event, result.getContent().get(0));
    }

    @Test
    @DisplayName("should list events filtered by status")
    void shouldListEventsFilteredByStatus() {
      Pageable pageable = PageRequest.of(0, 10);
      Page<Event> expectedPage = new PageImpl<>(List.of(event));

      when(eventRepository.findByOrganizerIdAndStatus(organizerId, EventStatusEnum.DRAFT, pageable))
          .thenReturn(expectedPage);

      Page<Event> result = eventService.listEventsForOrganizerByStatus(
          organizerId, EventStatusEnum.DRAFT, pageable);

      assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("should count events by status")
    void shouldCountEventsByStatus() {
      when(eventRepository.countByOrganizerIdAndStatus(organizerId, EventStatusEnum.PUBLISHED))
          .thenReturn(5L);

      long count = eventService.countEventsForOrganizerByStatus(organizerId, EventStatusEnum.PUBLISHED);

      assertEquals(5L, count);
    }
  }

  @Nested
  @DisplayName("getEventForOrganizer")
  class GetEventTests {

    @Test
    @DisplayName("should return event when found")
    void shouldReturnEventWhenFound() {
      when(eventRepository.findByIdAndOrganizerId(eventId, organizerId))
          .thenReturn(Optional.of(event));

      Optional<Event> result = eventService.getEventForOrganizer(organizerId, eventId);

      assertTrue(result.isPresent());
      assertEquals(event, result.get());
    }

    @Test
    @DisplayName("should return empty when event not found")
    void shouldReturnEmptyWhenEventNotFound() {
      when(eventRepository.findByIdAndOrganizerId(eventId, organizerId))
          .thenReturn(Optional.empty());

      Optional<Event> result = eventService.getEventForOrganizer(organizerId, eventId);

      assertFalse(result.isPresent());
    }
  }

  @Nested
  @DisplayName("updateEventForOrganizer")
  class UpdateEventTests {

    @Test
    @DisplayName("should update event successfully")
    void shouldUpdateEventSuccessfully() {
      UUID ticketTypeId = UUID.randomUUID();
      TicketType existingTicketType = new TicketType();
      existingTicketType.setId(ticketTypeId);
      existingTicketType.setName("General");
      existingTicketType.setPrice(50.0);
      existingTicketType.setEvent(event);
      event.setTicketTypes(new ArrayList<>(List.of(existingTicketType)));

      UpdateTicketTypeRequest updateTT = new UpdateTicketTypeRequest();
      updateTT.setId(ticketTypeId);
      updateTT.setName("General Updated");
      updateTT.setPrice(60.0);

      UpdateEventRequest request = new UpdateEventRequest();
      request.setId(eventId);
      request.setName("Updated Event");
      request.setVenue("Updated Venue");
      request.setStatus(EventStatusEnum.PUBLISHED);
      request.setTicketTypes(List.of(updateTT));

      when(eventRepository.findByIdAndOrganizerId(eventId, organizerId))
          .thenReturn(Optional.of(event));
      when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

      Event result = eventService.updateEventForOrganizer(organizerId, eventId, request);

      assertEquals("Updated Event", result.getName());
      assertEquals("Updated Venue", result.getVenue());
      assertEquals(EventStatusEnum.PUBLISHED, result.getStatus());
      assertEquals("General Updated", result.getTicketTypes().get(0).getName());
      assertEquals(60.0, result.getTicketTypes().get(0).getPrice());
    }

    @Test
    @DisplayName("should throw EventUpdateException when ID is null")
    void shouldThrowEventUpdateExceptionWhenIdIsNull() {
      UpdateEventRequest request = new UpdateEventRequest();
      request.setId(null);

      assertThrows(EventUpdateException.class,
          () -> eventService.updateEventForOrganizer(organizerId, eventId, request));
    }

    @Test
    @DisplayName("should throw EventUpdateException when ID mismatch")
    void shouldThrowEventUpdateExceptionWhenIdMismatch() {
      UpdateEventRequest request = new UpdateEventRequest();
      request.setId(UUID.randomUUID());

      assertThrows(EventUpdateException.class,
          () -> eventService.updateEventForOrganizer(organizerId, eventId, request));
    }

    @Test
    @DisplayName("should throw EventNotFoundException when event not found")
    void shouldThrowEventNotFoundExceptionWhenNotFound() {
      UpdateEventRequest request = new UpdateEventRequest();
      request.setId(eventId);
      request.setTicketTypes(List.of());

      when(eventRepository.findByIdAndOrganizerId(eventId, organizerId))
          .thenReturn(Optional.empty());

      assertThrows(EventNotFoundException.class,
          () -> eventService.updateEventForOrganizer(organizerId, eventId, request));
    }

    @Test
    @DisplayName("should add new ticket type when ID is null")
    void shouldAddNewTicketType() {
      event.setTicketTypes(new ArrayList<>());

      UpdateTicketTypeRequest newTT = new UpdateTicketTypeRequest();
      newTT.setId(null);
      newTT.setName("New Type");
      newTT.setPrice(25.0);

      UpdateEventRequest request = new UpdateEventRequest();
      request.setId(eventId);
      request.setName("Event");
      request.setVenue("Venue");
      request.setStatus(EventStatusEnum.DRAFT);
      request.setTicketTypes(List.of(newTT));

      when(eventRepository.findByIdAndOrganizerId(eventId, organizerId))
          .thenReturn(Optional.of(event));
      when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

      Event result = eventService.updateEventForOrganizer(organizerId, eventId, request);

      assertEquals(1, result.getTicketTypes().size());
      assertEquals("New Type", result.getTicketTypes().get(0).getName());
    }

    @Test
    @DisplayName("should throw TicketTypeNotFoundException for unknown ticket type ID")
    void shouldThrowTicketTypeNotFoundForUnknownId() {
      event.setTicketTypes(new ArrayList<>());

      UpdateTicketTypeRequest unknownTT = new UpdateTicketTypeRequest();
      unknownTT.setId(UUID.randomUUID());
      unknownTT.setName("Unknown");
      unknownTT.setPrice(25.0);

      UpdateEventRequest request = new UpdateEventRequest();
      request.setId(eventId);
      request.setName("Event");
      request.setVenue("Venue");
      request.setStatus(EventStatusEnum.DRAFT);
      request.setTicketTypes(List.of(unknownTT));

      when(eventRepository.findByIdAndOrganizerId(eventId, organizerId))
          .thenReturn(Optional.of(event));

      assertThrows(TicketTypeNotFoundException.class,
          () -> eventService.updateEventForOrganizer(organizerId, eventId, request));
    }
  }

  @Nested
  @DisplayName("deleteEventForOrganizer")
  class DeleteEventTests {

    @Test
    @DisplayName("should delete event when found")
    void shouldDeleteEventWhenFound() {
      when(eventRepository.findByIdAndOrganizerId(eventId, organizerId))
          .thenReturn(Optional.of(event));

      eventService.deleteEventForOrganizer(organizerId, eventId);

      verify(eventRepository).delete(event);
    }

    @Test
    @DisplayName("should not delete when event not found")
    void shouldNotDeleteWhenNotFound() {
      when(eventRepository.findByIdAndOrganizerId(eventId, organizerId))
          .thenReturn(Optional.empty());

      eventService.deleteEventForOrganizer(organizerId, eventId);

      verify(eventRepository, never()).delete(any());
    }
  }

  @Nested
  @DisplayName("published events")
  class PublishedEventTests {

    @Test
    @DisplayName("should list published events")
    void shouldListPublishedEvents() {
      Pageable pageable = PageRequest.of(0, 10);
      event.setStatus(EventStatusEnum.PUBLISHED);
      Page<Event> expectedPage = new PageImpl<>(List.of(event));

      when(eventRepository.findByStatus(EventStatusEnum.PUBLISHED, pageable))
          .thenReturn(expectedPage);

      Page<Event> result = eventService.listPublishedEvents(pageable);

      assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("should search published events")
    void shouldSearchPublishedEvents() {
      Pageable pageable = PageRequest.of(0, 10);
      Page<Event> expectedPage = new PageImpl<>(List.of(event));

      when(eventRepository.searchEvents("test", pageable)).thenReturn(expectedPage);

      Page<Event> result = eventService.searchPublishedEvents("test", pageable);

      assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("should get published event by ID")
    void shouldGetPublishedEventById() {
      when(eventRepository.findByIdAndStatus(eventId, EventStatusEnum.PUBLISHED))
          .thenReturn(Optional.of(event));

      Optional<Event> result = eventService.getPublishedEvent(eventId);

      assertTrue(result.isPresent());
    }
  }
}
