package com.fullstack.venuesync.staff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.events.domain.EventStatusEnum;
import com.fullstack.venuesync.events.repository.EventRepository;
import com.fullstack.venuesync.shared.domain.User;
import com.fullstack.venuesync.shared.domain.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/** The door permission is two queries; they run against a real database, not a mock. */
@DataJpaTest
class EventStaffQueriesTest {

  @Autowired private TestEntityManager entityManager;
  @Autowired private UserRepository userRepository;
  @Autowired private EventRepository eventRepository;

  private User user(String name) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setName(name);
    user.setEmail(name + "@example.com");
    return entityManager.persist(user);
  }

  private Event event(User organizer) {
    Event event = new Event();
    event.setName("Show");
    event.setVenue("Hall");
    event.setStatus(EventStatusEnum.PUBLISHED);
    event.setOrganizer(organizer);
    return entityManager.persist(event);
  }

  @Test
  void staffMembershipIsPerEvent() {
    User organizer = user("org");
    User staff = user("staff");
    Event worked = event(organizer);
    Event other = event(organizer);
    staff.getStaffingEvents().add(worked);
    entityManager.flush();

    assertTrue(userRepository.isStaffOf(staff.getId(), worked.getId()));
    assertFalse(userRepository.isStaffOf(staff.getId(), other.getId()));
    assertFalse(userRepository.isStaffOf(organizer.getId(), worked.getId())); // organizing isn't staffing...
    assertTrue(eventRepository.existsByIdAndOrganizerId(worked.getId(), organizer.getId())); // ...it's checked here
    assertFalse(eventRepository.existsByIdAndOrganizerId(worked.getId(), staff.getId()));
  }

  @Test
  void scannableEventsAreOrganizedPlusStaffedAndPublished() {
    User organizer = user("org");
    User staff = user("staff");
    Event staffed = event(organizer);
    Event ownedByStaff = event(staff);
    Event draft = event(staff);
    draft.setStatus(EventStatusEnum.DRAFT);
    event(organizer); // neither organized nor staffed by `staff`
    staff.getStaffingEvents().add(staffed);
    entityManager.flush();

    var ids = eventRepository.findScannableBy(staff.getId(), EventStatusEnum.PUBLISHED).stream().map(Event::getId).toList();

    assertEquals(java.util.Set.of(staffed.getId(), ownedByStaff.getId()), java.util.Set.copyOf(ids));
    assertEquals(2, ids.size()); // DISTINCT: no duplicate rows from the staff join
  }
}
