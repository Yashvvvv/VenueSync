package com.fullstack.venuesync.events.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.events.domain.EventStatusEnum;
import com.fullstack.venuesync.events.repository.EventRepository;
import com.fullstack.venuesync.shared.domain.User;
import com.fullstack.venuesync.shared.domain.UserRepository;
import com.fullstack.venuesync.staff.repository.StaffInviteRepository;
import com.fullstack.venuesync.staff.service.StaffInviteService;
import com.fullstack.venuesync.tickets.domain.Ticket;
import com.fullstack.venuesync.tickets.domain.TicketStatusEnum;
import com.fullstack.venuesync.tickets.domain.TicketType;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * Found on the live site: deleting an event that had door staff or an invite failed with a 500, because those rows
 * point at the event outside its cascade. Runs against a real database, where the foreign keys are enforced.
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
  @Autowired private TestEntityManager entityManager;

  private User user(String name) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setName(name);
    user.setEmail(name + "@example.com");
    return entityManager.persist(user);
  }

  @Test
  void anEventWithStaffInvitesAndTicketsCanBeDeleted() {
    User organizer = user("org");
    User staff = user("staff");
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
    Ticket ticket = new Ticket();
    ticket.setStatus(TicketStatusEnum.USED);
    ticket.setTicketType(type);
    ticket.setPurchaser(staff);
    type.getTickets().add(ticket);
    entityManager.persist(ticket);
    entityManager.flush();

    // One invite redeemed (the user is now staff) and one still open: both point at the event.
    staffInviteService.acceptInvite(staff.getId(), staffInviteService.createInvite(organizer.getId(), event.getId()).code());
    staffInviteService.createInvite(organizer.getId(), event.getId());
    entityManager.flush();
    entityManager.clear();
    assertTrue(userRepository.isStaffOf(staff.getId(), event.getId()));

    eventService.deleteEventForOrganizer(organizer.getId(), event.getId());
    entityManager.flush(); // the foreign keys are checked here; this threw before the fix

    assertFalse(eventRepository.existsById(event.getId()));
    assertTrue(inviteRepository.findAll().isEmpty());
    assertFalse(userRepository.isStaffOf(staff.getId(), event.getId()));
    assertTrue(userRepository.existsById(staff.getId())); // the staff member's account stays
  }
}
