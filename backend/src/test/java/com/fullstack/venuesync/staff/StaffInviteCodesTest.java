package com.fullstack.venuesync.staff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fullstack.venuesync.staff.service.StaffInviteCodes;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StaffInviteCodesTest {

  @Test
  void generatedCodesRoundTripThroughTheDisplayForm() {
    for (int i = 0; i < 1000; i++) {
      String code = StaffInviteCodes.generate();
      assertEquals(Optional.of(code), StaffInviteCodes.normalize(StaffInviteCodes.format(code)));
    }
  }

  @Test
  void readingToleratesHowPeopleTypeCodes() {
    assertEquals(Optional.of("K7Q2M9XH4P"), StaffInviteCodes.normalize(" k7q2m-9xh4p "));
    assertEquals(Optional.of("10Q2M9XH41"), StaffInviteCodes.normalize("IOQ2M 9XH4L")); // I/L -> 1, O -> 0
  }

  @Test
  void rejectsWhatCannotBeACode() {
    assertTrue(StaffInviteCodes.normalize("K7Q2M-9XH4").isEmpty());   // too short
    assertTrue(StaffInviteCodes.normalize("K7Q2M-9XH4U").isEmpty());  // U isn't in the alphabet
    assertTrue(StaffInviteCodes.normalize(null).isEmpty());
  }
}
