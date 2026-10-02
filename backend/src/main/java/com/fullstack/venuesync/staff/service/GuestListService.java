package com.fullstack.venuesync.staff.service;

import com.fullstack.venuesync.staff.dto.GuestDto;
import com.fullstack.venuesync.tickets.domain.Ticket;
import com.fullstack.venuesync.tickets.domain.TicketCodes;
import com.fullstack.venuesync.tickets.repository.TicketRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Door fallback when a QR code won't scan: find the guest by name, email or ticket code, then check them in. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuestListService {

  static final int MIN_QUERY = 2;
  static final int MAX_RESULTS = 20;
  /** Can never match a UUID's text, so "search by code" is off for queries that aren't hex. */
  private static final String NO_CODE = "#";

  private final TicketRepository ticketRepository;
  private final EventStaffService eventStaffService;

  public List<GuestDto> search(UUID userId, UUID eventId, String query) {
    eventStaffService.requireCanScan(userId, eventId);
    // Wildcards are stripped so a query can't turn into "list everyone"; a short query lists nobody.
    String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT).replaceAll("[%_]", "");
    if (q.length() < MIN_QUERY) {
      return List.of();
    }
    String hex = q.replaceAll("[\\s-]", "");
    String codePrefix = hex.matches("[0-9a-f]{1," + TicketCodes.LENGTH + "}") ? hex + "%" : NO_CODE;
    return ticketRepository.searchGuests(eventId, "%" + q + "%", codePrefix, PageRequest.of(0, MAX_RESULTS))
        .stream()
        .map(GuestListService::toGuest)
        .toList();
  }

  private static GuestDto toGuest(Ticket ticket) {
    return new GuestDto(
        ticket.getId(),
        TicketCodes.of(ticket.getId()),
        ticket.getPurchaser().getName(),
        maskEmail(ticket.getPurchaser().getEmail()),
        ticket.getTicketType().getName(),
        ticket.getStatus());
  }

  /** yash@gmail.com -> ya***@gmail.com. */
  static String maskEmail(String email) {
    if (email == null) {
      return null;
    }
    int at = email.indexOf('@');
    if (at <= 0) {
      return "***";
    }
    return email.substring(0, Math.min(2, at)) + "***" + email.substring(at);
  }
}
