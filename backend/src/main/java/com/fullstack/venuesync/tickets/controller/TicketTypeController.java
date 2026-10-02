package com.fullstack.venuesync.tickets.controller;

import static com.fullstack.venuesync.shared.security.JwtUtil.parseUserId;

import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fullstack.venuesync.tickets.domain.Ticket;
import com.fullstack.venuesync.tickets.dto.GetTicketResponseDto;
import com.fullstack.venuesync.tickets.mapper.TicketMapper;
import com.fullstack.venuesync.tickets.service.TicketTypeService;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1/events/{eventId}/ticket-types")
public class TicketTypeController {

  private final TicketTypeService ticketTypeService;
  private final TicketMapper ticketMapper;

  /**
   * Requires an {@code Idempotency-Key} header (UUID, one per purchase attempt). Retrying with the
   * same key returns the same ticket, so clients can safely retry timeouts and dropped connections.
   */
  @PostMapping(path = "/{ticketTypeId}/tickets")
  public ResponseEntity<GetTicketResponseDto> purchaseTicket(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID eventId,
      @PathVariable UUID ticketTypeId,
      @RequestHeader("Idempotency-Key") UUID idempotencyKey
  ) {
    Ticket ticket = ticketTypeService.purchaseTicket(parseUserId(jwt), eventId, ticketTypeId, idempotencyKey);
    return ResponseEntity
        .created(URI.create("/api/v1/tickets/" + ticket.getId()))
        .body(ticketMapper.toGetTicketResponseDto(ticket));
  }
}
