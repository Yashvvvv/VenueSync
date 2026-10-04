package com.fullstack.venuesync.tickets.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.events.domain.EventStatusEnum;
import com.fullstack.venuesync.tickets.domain.Ticket;
import com.fullstack.venuesync.tickets.domain.TicketStatusEnum;
import com.fullstack.venuesync.tickets.domain.TicketType;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/** The bulk cancel and the event's version are SQL and JPA behaviour, so they're tested against a real database. */
@DataJpaTest
class CancelEventTicketsTest {

  @Autowired private TicketRepository ticketRepository;
  @Autowired private TestEntityManager entityManager;

  private Event persistEvent() {
    Event event = new Event();
    event.setName("Show");
    event.setVenue("Hall");
    event.setStatus(EventStatusEnum.PUBLISHED);
    return entityManager.persistAndFlush(event);
  }

  private UUID persistTicket(Event event, TicketStatusEnum status) {
    TicketType type = new TicketType();
    type.setName("GA");
    type.setPrice(10.0);
    type.setEvent(event);
    entityManager.persist(type);
    Ticket ticket = new Ticket();
    ticket.setStatus(status);
    ticket.setTicketType(type);
    return entityManager.persistAndFlush(ticket).getId();
  }

  private TicketStatusEnum statusOf(UUID id) {
    return ticketRepository.findById(id).orElseThrow().getStatus();
  }

  @Test
  @DisplayName("cancels the event's unused tickets only: used ones and other events' stay as they are")
  void cancelsOnlyThisEventsUnusedTickets() {
    Event cancelled = persistEvent();
    Event other = persistEvent();
    UUID unused = persistTicket(cancelled, TicketStatusEnum.PURCHASED);
    UUID used = persistTicket(cancelled, TicketStatusEnum.USED);
    UUID elsewhere = persistTicket(other, TicketStatusEnum.PURCHASED);

    assertEquals(1, ticketRepository.moveStatusForEvent(
        cancelled.getId(), TicketStatusEnum.PURCHASED, TicketStatusEnum.CANCELLED));

    entityManager.clear();
    assertEquals(TicketStatusEnum.CANCELLED, statusOf(unused));
    assertEquals(TicketStatusEnum.USED, statusOf(used));
    assertEquals(TicketStatusEnum.PURCHASED, statusOf(elsewhere));
  }

  @Test
  @DisplayName("an event starts at version 0 and every change bumps it")
  void versionCountsChanges() {
    Event event = persistEvent();
    assertEquals(0L, event.getVersion());
    event.setName("Show, moved");
    entityManager.flush();
    assertEquals(1L, event.getVersion());
  }
}
