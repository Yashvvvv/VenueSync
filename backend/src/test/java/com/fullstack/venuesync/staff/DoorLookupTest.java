package com.fullstack.venuesync.staff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.events.domain.EventStatusEnum;
import com.fullstack.venuesync.shared.domain.User;
import com.fullstack.venuesync.staff.dto.GuestDto;
import com.fullstack.venuesync.staff.exception.NotEventStaffException;
import com.fullstack.venuesync.staff.service.EventStaffService;
import com.fullstack.venuesync.staff.service.GuestListService;
import com.fullstack.venuesync.tickets.domain.Ticket;
import com.fullstack.venuesync.tickets.domain.TicketCodes;
import com.fullstack.venuesync.tickets.domain.TicketStatusEnum;
import com.fullstack.venuesync.tickets.domain.TicketType;
import com.fullstack.venuesync.tickets.repository.TicketRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

/** Ticket-code lookup and the guest list, against a real database (the UUID-to-text cast must really work). */
@DataJpaTest
@Import({GuestListService.class, EventStaffService.class})
class DoorLookupTest {

  @Autowired private TestEntityManager entityManager;
  @Autowired private TicketRepository ticketRepository;
  @Autowired private GuestListService guestListService;

  private User organizer;
  private User stranger;
  private Event event;
  private Event otherEvent;
  private Ticket yashTicket;

  private User user(String name, String email) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setName(name);
    user.setEmail(email);
    return entityManager.persist(user);
  }

  private Event event(String name) {
    Event event = new Event();
    event.setName(name);
    event.setVenue("Hall");
    event.setStatus(EventStatusEnum.PUBLISHED);
    event.setOrganizer(organizer);
    return entityManager.persist(event);
  }

  private Ticket ticket(Event event, User buyer) {
    TicketType type = new TicketType();
    type.setName("VIP");
    type.setPrice(25.0);
    type.setEvent(event);
    entityManager.persist(type);
    Ticket ticket = new Ticket();
    ticket.setStatus(TicketStatusEnum.PURCHASED);
    ticket.setTicketType(type);
    ticket.setPurchaser(buyer);
    return entityManager.persist(ticket);
  }

  @BeforeEach
  void setUp() {
    organizer = user("Org", "org@example.com");
    stranger = user("Stranger", "s@example.com");
    event = event("Summer Vibes");
    otherEvent = event("Other");
    User yash = user("Yash Sharma", "yash@gmail.com");
    yashTicket = ticket(event, yash);
    ticket(event, user("Asha Rao", "asha@example.com"));
    ticket(otherEvent, yash);
    entityManager.flush();
    entityManager.clear();
  }

  private String prefix(Ticket t) {
    return TicketCodes.normalize(TicketCodes.of(t.getId())).orElseThrow();
  }

  @Test
  void ticketCodeFindsExactlyThatTicketWithinItsEvent() {
    List<Ticket> found = ticketRepository.findByEventAndCodePrefix(event.getId(), prefix(yashTicket), PageRequest.of(0, 2));
    assertEquals(List.of(yashTicket.getId()), found.stream().map(Ticket::getId).toList());
    assertTrue(ticketRepository.findByEventAndCodePrefix(otherEvent.getId(), prefix(yashTicket), PageRequest.of(0, 2)).isEmpty());
  }

  @Test
  void guestListSearchesNameEmailAndCodeAndMasksEmails() {
    List<GuestDto> byName = guestListService.search(organizer.getId(), event.getId(), "yash");
    assertEquals(1, byName.size());
    GuestDto guest = byName.get(0);
    assertEquals("Yash Sharma", guest.attendeeName());
    assertEquals("ya***@gmail.com", guest.attendeeEmail());
    assertEquals(TicketCodes.of(yashTicket.getId()), guest.ticketCode());
    assertEquals("VIP", guest.ticketTypeName());

    assertEquals(1, guestListService.search(organizer.getId(), event.getId(), "asha@example").size());
    assertEquals(yashTicket.getId(),
        guestListService.search(organizer.getId(), event.getId(), TicketCodes.of(yashTicket.getId())).get(0).ticketId());
  }

  @Test
  void guestListNeverDumpsEveryoneAndIsForStaffOnly() {
    assertTrue(guestListService.search(organizer.getId(), event.getId(), "y").isEmpty());   // too short
    assertTrue(guestListService.search(organizer.getId(), event.getId(), "%%").isEmpty());  // wildcards stripped
    assertThrows(NotEventStaffException.class, () -> guestListService.search(stranger.getId(), event.getId(), "yash"));
  }
}
