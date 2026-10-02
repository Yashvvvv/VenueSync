package com.fullstack.venuesync.staff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.events.domain.EventStatusEnum;
import com.fullstack.venuesync.events.exception.EventNotFoundException;
import com.fullstack.venuesync.shared.domain.User;
import com.fullstack.venuesync.shared.domain.UserRepository;
import com.fullstack.venuesync.staff.domain.StaffInvite;
import com.fullstack.venuesync.staff.dto.EventStaffMemberDto;
import com.fullstack.venuesync.staff.exception.StaffInviteExpiredException;
import com.fullstack.venuesync.staff.exception.StaffInviteNotFoundException;
import com.fullstack.venuesync.staff.exception.StaffInviteUsedException;
import com.fullstack.venuesync.staff.repository.StaffInviteRepository;
import com.fullstack.venuesync.staff.service.StaffInviteService;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

/** The whole invite flow against a real database: organizer invites, staff redeems, staff can scan. */
@DataJpaTest
@Import(StaffInviteService.class)
class StaffInviteFlowTest {

  @Autowired private StaffInviteService service;
  @Autowired private StaffInviteRepository inviteRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private TestEntityManager entityManager;

  private User organizer;
  private User staff;
  private User stranger;
  private Event event;

  private User user(String name) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setName(name);
    user.setEmail(name + "@example.com");
    return entityManager.persist(user);
  }

  @BeforeEach
  void setUp() {
    organizer = user("org");
    staff = user("staff");
    stranger = user("stranger");
    event = new Event();
    event.setName("Summer Vibes");
    event.setVenue("Hall");
    event.setStatus(EventStatusEnum.PUBLISHED);
    event.setOrganizer(organizer);
    entityManager.persist(event);
    entityManager.flush();
  }

  private void reload() {
    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void inviteMakesTheRedeemerStaffOfThatEventOnly() {
    String code = service.createInvite(organizer.getId(), event.getId()).code();
    assertTrue(code.matches("[0-9A-Z]{5}-[0-9A-Z]{5}"));

    var accepted = service.acceptInvite(staff.getId(), code.toLowerCase());
    reload();

    assertEquals(event.getId(), accepted.eventId());
    assertEquals("Summer Vibes", accepted.eventName());
    assertTrue(userRepository.isStaffOf(staff.getId(), event.getId()));
    assertEquals(
        java.util.List.of(new EventStaffMemberDto(staff.getId(), "staff", "staff@example.com")),
        service.listStaff(organizer.getId(), event.getId()));
  }

  @Test
  void aCodeWorksOnceButOpeningItAgainIsHarmless() {
    String code = service.createInvite(organizer.getId(), event.getId()).code();
    service.acceptInvite(staff.getId(), code);
    reload();

    service.acceptInvite(staff.getId(), code); // same person, same link: fine
    assertThrows(StaffInviteUsedException.class, () -> service.acceptInvite(stranger.getId(), code));
    reload();
    assertFalse(userRepository.isStaffOf(stranger.getId(), event.getId()));
  }

  @Test
  void expiredAndUnknownCodesAreRefused() {
    String code = service.createInvite(organizer.getId(), event.getId()).code();
    StaffInvite invite = inviteRepository.findAll().get(0);
    invite.setExpiresAt(LocalDateTime.now().minusMinutes(1));
    reload();

    assertThrows(StaffInviteExpiredException.class, () -> service.acceptInvite(staff.getId(), code));
    assertThrows(StaffInviteNotFoundException.class, () -> service.acceptInvite(staff.getId(), "AAAAA-AAAAA"));
    assertThrows(StaffInviteNotFoundException.class, () -> service.acceptInvite(staff.getId(), "nonsense"));
  }

  @Test
  void onlyTheOrganizerManagesTheTeam() {
    assertThrows(EventNotFoundException.class, () -> service.createInvite(stranger.getId(), event.getId()));
    assertThrows(EventNotFoundException.class, () -> service.listStaff(stranger.getId(), event.getId()));
  }

  @Test
  void removingStaffTakesTheDoorAway() {
    service.acceptInvite(staff.getId(), service.createInvite(organizer.getId(), event.getId()).code());
    reload();
    service.removeStaff(organizer.getId(), event.getId(), staff.getId());
    service.removeStaff(organizer.getId(), event.getId(), staff.getId()); // idempotent
    reload();
    assertFalse(userRepository.isStaffOf(staff.getId(), event.getId()));
  }
}
