package com.fullstack.venuesync.tickets.domain;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * The short code people read out at the door: the first 8 hex characters of the ticket id, shown as {@code F5A3-038B}.
 * Derived, so every ticket (old ones too) has one without a column or a backfill. It is only ever looked up
 * WITHIN one event; two tickets of the same event sharing it is a ~1-in-400,000 event for a 10,000-ticket show,
 * and is then answered "invalid" (use the QR or the guest list), never a guess.
 */
public final class TicketCodes {

  public static final int LENGTH = 8;

  private TicketCodes() {
  }

  public static String of(UUID ticketId) {
    String hex = ticketId.toString().substring(0, LENGTH).toUpperCase(Locale.ROOT);
    return hex.substring(0, 4) + "-" + hex.substring(4);
  }

  /** The lower-case hex prefix to look up, or empty when the input can't be a ticket code. */
  public static Optional<String> normalize(String input) {
    if (input == null) {
      return Optional.empty();
    }
    String hex = input.replaceAll("[\\s-]", "").toLowerCase(Locale.ROOT);
    return hex.matches("[0-9a-f]{" + LENGTH + "}") ? Optional.of(hex) : Optional.empty();
  }
}
