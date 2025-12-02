package com.fullstack.tickets.controllers;

import static com.fullstack.tickets.util.JwtUtil.parseUserId;

import com.fullstack.tickets.domain.dtos.GetTicketResponseDto;
import com.fullstack.tickets.domain.dtos.ListTicketResponseDto;
import com.fullstack.tickets.mappers.TicketMapper;
import com.fullstack.tickets.services.QrCodeService;
import com.fullstack.tickets.services.TicketService;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

  private final TicketService ticketService;
  private final TicketMapper ticketMapper;
  private final QrCodeService qrCodeService;

  @GetMapping
  public Page<ListTicketResponseDto> listTickets(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(required = false) String filter,
      Pageable pageable
  ) {
    UUID userId = parseUserId(jwt);
    
    if ("active".equalsIgnoreCase(filter)) {
      return ticketService.listActiveTicketsForUser(userId, pageable)
          .map(ticketMapper::toListTicketResponseDto);
    } else if ("past".equalsIgnoreCase(filter)) {
      return ticketService.listPastTicketsForUser(userId, pageable)
          .map(ticketMapper::toListTicketResponseDto);
    }
    
    // Default: return all tickets
    return ticketService.listTicketsForUser(userId, pageable)
        .map(ticketMapper::toListTicketResponseDto);
  }

  @GetMapping(path = "/{ticketId}")
  public ResponseEntity<GetTicketResponseDto> getTicket(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID ticketId
  ) {
    return ticketService
        .getTicketForUser(parseUserId(jwt), ticketId)
        .map(ticketMapper::toGetTicketResponseDto)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping(path = "/{ticketId}/qr-codes")
  public ResponseEntity<byte[]> getTicketQrCode(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID ticketId
  ) {
    byte[] qrCodeImage = qrCodeService.getQrCodeImageForUserAndTicket(
        parseUserId(jwt),
        ticketId
    );

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.IMAGE_PNG);
    headers.setContentLength(qrCodeImage.length);

    return ResponseEntity.ok()
        .headers(headers)
        .body(qrCodeImage);
  }

}
