package com.fullstack.venuesync.tickets.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TicketCodesTest {

  private final UUID id = UUID.fromString("f5a3038b-3412-46f9-8f10-33fc236d6b17");

  @Test
  void codeIsTheFirstEightHexCharacters() {
    assertEquals("F5A3-038B", TicketCodes.of(id));
  }

  @Test
  void typedCodesNormalizeToTheLookupPrefix() {
    assertEquals(Optional.of("f5a3038b"), TicketCodes.normalize("F5A3-038B"));
    assertEquals(Optional.of("f5a3038b"), TicketCodes.normalize(" f5a3 038b "));
    assertTrue(TicketCodes.normalize("F5A3-038").isEmpty());   // too short
    assertTrue(TicketCodes.normalize("G5A3-038B").isEmpty());  // not hex
    assertTrue(TicketCodes.normalize(null).isEmpty());
  }
}
