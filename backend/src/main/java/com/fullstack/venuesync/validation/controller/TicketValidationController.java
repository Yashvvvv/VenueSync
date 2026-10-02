package com.fullstack.venuesync.validation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import static com.fullstack.venuesync.shared.security.JwtUtil.parseUserId;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fullstack.venuesync.validation.dto.TicketValidationRequestDto;
import com.fullstack.venuesync.validation.dto.TicketValidationResponseDto;
import com.fullstack.venuesync.validation.domain.TicketValidation;
import com.fullstack.venuesync.validation.domain.TicketValidationMethod;
import com.fullstack.venuesync.validation.domain.TicketValidationStatusEnum;
import com.fullstack.venuesync.validation.mapper.TicketValidationMapper;
import com.fullstack.venuesync.validation.service.TicketValidationService;

import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/ticket-validations")
@RequiredArgsConstructor
public class TicketValidationController {

  private final TicketValidationService ticketValidationService;
  private final TicketValidationMapper ticketValidationMapper;

  @PostMapping
  public ResponseEntity<TicketValidationResponseDto> validateTicket(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody TicketValidationRequestDto ticketValidationRequestDto,
      // Optional so the web dashboard keeps working; the app sends one per scan, the same one on retries.
      @RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey
  ){
    TicketValidationMethod method = ticketValidationRequestDto.getMethod();
    TicketValidation ticketValidation;
    
    // Parse UUID - if invalid format, return INVALID status
    UUID id;
    try {
      id = UUID.fromString(ticketValidationRequestDto.getId());
    } catch (IllegalArgumentException | NullPointerException e) {
      // Invalid UUID format - return INVALID response
      TicketValidationResponseDto invalidResponse = new TicketValidationResponseDto();
      invalidResponse.setStatus(TicketValidationStatusEnum.INVALID);
      return ResponseEntity.ok(invalidResponse);
    }
    
    if(TicketValidationMethod.MANUAL.equals(method)) {
      ticketValidation = ticketValidationService.validateTicketManually(id, parseUserId(jwt), idempotencyKey, ticketValidationRequestDto.getEventId());
    } else {
      ticketValidation = ticketValidationService.validateTicketByQrCode(id, parseUserId(jwt), idempotencyKey, ticketValidationRequestDto.getEventId());
    }
    return ResponseEntity.ok(
        ticketValidationMapper.toTicketValidationResponseDto(ticketValidation)
    );
  }
}
