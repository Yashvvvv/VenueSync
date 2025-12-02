package com.fullstack.tickets.controllers;

import com.fullstack.tickets.domain.dtos.TicketValidationRequestDto;
import com.fullstack.tickets.domain.dtos.TicketValidationResponseDto;
import com.fullstack.tickets.domain.entities.TicketValidation;
import com.fullstack.tickets.domain.entities.TicketValidationMethod;
import com.fullstack.tickets.domain.entities.TicketValidationStatusEnum;
import com.fullstack.tickets.mappers.TicketValidationMapper;
import com.fullstack.tickets.services.TicketValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/ticket-validations")
@RequiredArgsConstructor
public class TicketValidationController {

  private final TicketValidationService ticketValidationService;
  private final TicketValidationMapper ticketValidationMapper;

  @PostMapping
  public ResponseEntity<TicketValidationResponseDto> validateTicket(
      @RequestBody TicketValidationRequestDto ticketValidationRequestDto
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
      ticketValidation = ticketValidationService.validateTicketManually(id);
    } else {
      ticketValidation = ticketValidationService.validateTicketByQrCode(id);
    }
    return ResponseEntity.ok(
        ticketValidationMapper.toTicketValidationResponseDto(ticketValidation)
    );
  }
}
