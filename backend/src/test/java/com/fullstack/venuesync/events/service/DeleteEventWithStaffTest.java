package com.fullstack.venuesync.events.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.events.domain.EventStatusEnum;
import com.fullstack.venuesync.events.exception.EventHasSalesException;
import com.fullstack.venuesync.events.repository.EventRepository;
import com.fullstack.venuesync.shared.domain.User;
import com.fullstack.venuesync.shared.domain.UserRepository;
import com.fullstack.venuesync.staff.repository.StaffInviteRepository;
import com.fullstack.venuesync.staff.service.StaffInviteService;
import com.fullstack.venuesync.tickets.domain.Ticket;
import com.fullstack.venuesync.tickets.domain.TicketStatusEnum;
import com.fullstack.venuesync.tickets.domain.TicketType;
import com.fullstack.venuesync.tickets.repository.TicketRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * Deleting an event against a real database, where the foreign keys are enforced.
 * <ul>
 *   <li>Found on the live site: an event with door staff or an invite failed to delete with a 500, because those rows
 *       point at the event outside its cascade.</li>
 *   <li>An event with tickets must never be deleted: the cascade took every buyer's ticket with it.</li>
 * </ul>
 */
@DataJpaTest
// The configured test database (NON_KEYWORDS=VALUE): the default replacement can't create qr_codes, which the
// ticket cascade touches.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({EventServiceImpl.class, StaffInviteService.class})
class DeleteEventWithStaffTest {

  @Autowired private EventServiceImpl eventService;
  @Autowired private StaffInviteService staffInviteService;
  @Autowired private EventRepository eventRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private StaffInviteRepository inviteRepository;
  @Autowired private TicketRepository ticketRepository;
  @Autowired private TestEntityManager entityManager;

  private User user(String name) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setName(name);
    user.setEmail(name + "@example.com");
    return entityManager.persist(user);
  }

  private Event eventWithType(User organizer) {
    Event event = new Event();
    event.setName("Show");
    event.setVenue("Hall");
    event.setStatus(EventStatusEnum.PUBLISHED);
    event.setOrganizer(organizer);
    entityManager.persist(event);
    TicketType type = new TicketType();
    type.setName("GA");
    type.setPrice(0.0);
    type.setEvent(event);
    event.getTicketTypes().add(type);
    entityManager.persist(type);
    return event;
  }

  @Test
  void anEventWithStaffAndInvitesButNoTicketsCanBeDeleted() {
    User organizer = user("org");
    User staff = user("staff");
    Event event = eventWithType(organizer);
    entityManager.flush();

    // One invite redeemed (the user is now staff) and one still open: both point at the event.
    staffInviteService.acceptInvite(staff.getId(), staffInviteService.createInvite(organizer.getId(), event.getId()).code());
    staffInviteService.createInvite(organizer.getId(), event.getId());
    entityManager.flush();
    entityManager.clear();
    assertTrue(userRepository.isStaffOf(staff.getId(), event.getId()));

    eventService.deleteEventForOrganizer(organizer.getId(), event.getId());
    entityManager.flush(); // the foreign keys are checked here; this threw before the staff fix

    assertFalse(eventRepository.existsById(event.getId()));
    assertTrue(inviteRepository.findAll().isEmpty());
    assertFalse(userRepository.isStaffOf(staff.getId(), event.getId()));
    assertTrue(userRepository.existsById(staff.getId())); // the staff member's account stays
  }

  @Test
  void anEventWithTicketsIsRefusedAndTheTicketsSurvive() {
    User organizer = user("org");
    User buyer = user("buyer");
    Event event = eventWithType(organizer);
    TicketType type = event.getTicketTypes().get(0);
    Ticket ticket = new Ticket();
    ticket.setStatus(TicketStatusEnum.USED);
    ticket.setTicketType(type);
    ticket.setPurchaser(buyer);
    type.getTickets().add(ticket);
    entityManager.persist(ticket);
    entityManager.flush();
    entityManager.clear();

    assertThrows(EventHasSalesException.class,
        () -> eventService.deleteEventForOrganizer(organizer.getId(), event.getId()));
    entityManager.flush();

    assertTrue(eventRepository.existsById(event.getId()));
    assertTrue(ticketRepository.existsById(ticket.getId()));
  }
}
