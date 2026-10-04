package com.fullstack.venuesync.events.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fullstack.venuesync.events.domain.CreateEventRequest;
import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.events.domain.EventStatusEnum;
import com.fullstack.venuesync.events.domain.UpdateEventRequest;
import com.fullstack.venuesync.events.exception.EventInvalidException;
import com.fullstack.venuesync.events.exception.StatusChangeInvalidException;
import com.fullstack.venuesync.events.repository.EventRepository;
import com.fullstack.venuesync.shared.domain.User;
import com.fullstack.venuesync.shared.domain.UserRepository;
import com.fullstack.venuesync.staff.repository.StaffInviteRepository;
import com.fullstack.venuesync.tickets.domain.CreateTicketTypeRequest;
import com.fullstack.venuesync.tickets.domain.TicketStatusEnum;
import com.fullstack.venuesync.tickets.domain.TicketType;
import com.fullstack.venuesync.tickets.domain.UpdateTicketTypeRequest;
import com.fullstack.venuesync.tickets.repository.TicketRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** What an organizer may schedule and which status moves are allowed, checked before anything changes. */
@ExtendWith(MockitoExtension.class)
class EventRulesTest {

  @Mock private UserRepository userRepository;
  @Mock private EventRepository eventRepository;
  @Mock private StaffInviteRepository staffInviteRepository;
  @Mock private TicketRepository ticketRepository;
  @InjectMocks private EventServiceImpl service;

  private final UUID organizerId = UUID.randomUUID();
  private final UUID eventId = UUID.randomUUID();
  private final UUID typeId = UUID.randomUUID();
  private final LocalDateTime at = LocalDateTime.of(2026, 11, 20, 19, 0);
  private Event event;

  @BeforeEach
  void setUp() {
    event = new Event();
    event.setId(eventId);
    event.setName("Show");
    event.setVenue("Hall");
    TicketType type = new TicketType();
    type.setId(typeId);
    type.setName("GA");
    type.setPrice(10.0);
    type.setEvent(event);
    event.setTicketTypes(new ArrayList<>(List.of(type)));
  }

  private UpdateEventRequest update(EventStatusEnum status, LocalDateTime start, LocalDateTime end,
      LocalDateTime salesStart, LocalDateTime salesEnd) {
    UpdateTicketTypeRequest keep = new UpdateTicketTypeRequest();
    keep.setId(typeId);
    keep.setName("GA");
    keep.setPrice(10.0);
    UpdateEventRequest request = new UpdateEventRequest();
    request.setId(eventId);
    request.setName("Show");
    request.setVenue("Hall");
    request.setStatus(status);
    request.setStart(start);
    request.setEnd(end);
    request.setSalesStart(salesStart);
    request.setSalesEnd(salesEnd);
    request.setTicketTypes(List.of(keep));
    return request;
  }

  private void given(EventStatusEnum status, long issued) {
    event.setStatus(status);
    when(eventRepository.findByIdAndOrganizerId(eventId, organizerId)).thenReturn(Optional.of(event));
    when(ticketRepository.countSoldByTicketTypeForEvent(eventId))
        .thenReturn(issued == 0 ? List.of() : List.<Object[]>of(new Object[] {typeId, issued}));
  }

  @Test
  void anEventThatEndsBeforeItStartsIsRejectedOnTheEndField() {
    given(EventStatusEnum.DRAFT, 0);
    EventInvalidException ex = assertThrows(EventInvalidException.class, () -> service.updateEventForOrganizer(
        organizerId, eventId, update(EventStatusEnum.DRAFT, at, at.minusHours(1), null, null)));
    assertEquals("end", ex.getField());
    verify(eventRepository, never()).save(any());
  }

  @Test
  void salesThatEndAfterTheEventAreRejectedOnTheSalesEndField() {
    given(EventStatusEnum.DRAFT, 0);
    EventInvalidException ex = assertThrows(EventInvalidException.class, () -> service.updateEventForOrganizer(
        organizerId, eventId, update(EventStatusEnum.DRAFT, at, at.plusHours(4), at.minusDays(7), at.plusDays(1))));
    assertEquals("salesEnd", ex.getField());
  }

  @Test
  void aPublishedEventWithTicketsCannotGoBackToDraft() {
    given(EventStatusEnum.PUBLISHED, 3);
    assertThrows(StatusChangeInvalidException.class, () -> service.updateEventForOrganizer(
        organizerId, eventId, update(EventStatusEnum.DRAFT, null, null, null, null)));
  }

  @Test
  void aPublishedEventWithoutTicketsCanGoBackToDraft() {
    given(EventStatusEnum.PUBLISHED, 0);
    when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));
    assertDoesNotThrow(() -> service.updateEventForOrganizer(
        organizerId, eventId, update(EventStatusEnum.DRAFT, null, null, null, null)));
  }

  @Test
  void cancelledAndCompletedAreFinal() {
    given(EventStatusEnum.CANCELLED, 0);
    assertThrows(StatusChangeInvalidException.class, () -> service.updateEventForOrganizer(
        organizerId, eventId, update(EventStatusEnum.PUBLISHED, null, null, null, null)));
    event.setStatus(EventStatusEnum.COMPLETED);
    assertThrows(StatusChangeInvalidException.class, () -> service.updateEventForOrganizer(
        organizerId, eventId, update(EventStatusEnum.PUBLISHED, null, null, null, null)));
  }

  @Test
  void cancellingVoidsTheTicketsNobodyHasUsed() {
    given(EventStatusEnum.PUBLISHED, 3);
    when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));
    service.updateEventForOrganizer(organizerId, eventId, update(EventStatusEnum.CANCELLED, at, at.plusHours(4), null, null));
    verify(ticketRepository).moveStatusForEvent(eq(eventId), eq(TicketStatusEnum.PURCHASED), eq(TicketStatusEnum.CANCELLED));
  }

  @Test
  void editingAnEventThatIsAlreadyCancelledMovesNoTickets() {
    given(EventStatusEnum.CANCELLED, 3);
    when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));
    service.updateEventForOrganizer(organizerId, eventId, update(EventStatusEnum.CANCELLED, null, null, null, null));
    verify(ticketRepository, never()).moveStatusForEvent(any(), any(), any());
  }

  @Test
  void aNewEventStartsAsDraftOrPublished() {
    CreateEventRequest request = new CreateEventRequest();
    request.setName("Show");
    request.setVenue("Hall");
    request.setStatus(EventStatusEnum.COMPLETED);
    request.setTicketTypes(List.of(new CreateTicketTypeRequest("GA", 10.0, null, null)));
    EventInvalidException ex = assertThrows(EventInvalidException.class,
        () -> service.createEvent(organizerId, request));
    assertEquals("status", ex.getField());
    verify(userRepository, never()).findById(any());
  }

  @Test
  void aValidNewScheduleIsAccepted() {
    CreateEventRequest request = new CreateEventRequest();
    request.setName("Show");
    request.setVenue("Hall");
    request.setStatus(EventStatusEnum.PUBLISHED);
    request.setStart(at);
    request.setEnd(at.plusHours(4));
    request.setSalesStart(at.minusDays(10));
    request.setSalesEnd(at.plusHours(1));
    request.setTicketTypes(List.of(new CreateTicketTypeRequest("GA", 10.0, null, null)));
    User organizer = new User();
    organizer.setId(organizerId);
    when(userRepository.findById(organizerId)).thenReturn(Optional.of(organizer));
    when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));
    assertDoesNotThrow(() -> service.createEvent(organizerId, request));
  }
}
