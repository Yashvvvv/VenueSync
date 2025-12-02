package com.fullstack.tickets.services.impl;

import com.fullstack.tickets.domain.entities.QrCode;
import com.fullstack.tickets.domain.entities.QrCodeStatusEnum;
import com.fullstack.tickets.domain.entities.Ticket;
import com.fullstack.tickets.domain.entities.TicketStatusEnum;
import com.fullstack.tickets.domain.entities.TicketValidation;
import com.fullstack.tickets.domain.entities.TicketValidationMethod;
import com.fullstack.tickets.domain.entities.TicketValidationStatusEnum;
import com.fullstack.tickets.repositories.QrCodeRepository;
import com.fullstack.tickets.repositories.TicketRepository;
import com.fullstack.tickets.repositories.TicketValidationRepository;
import com.fullstack.tickets.services.TicketValidationService;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketValidationServiceImpl implements TicketValidationService {

  private final QrCodeRepository qrCodeRepository;
  private final TicketValidationRepository ticketValidationRepository;
  private final TicketRepository ticketRepository;

  @Override
  public TicketValidation validateTicketByQrCode(UUID qrCodeId) {
    Optional<QrCode> qrCodeOpt = qrCodeRepository.findByIdAndStatus(qrCodeId, QrCodeStatusEnum.ACTIVE);
    
    // If QR code not found or inactive, return INVALID status
    if (qrCodeOpt.isEmpty()) {
      TicketValidation invalidValidation = new TicketValidation();
      invalidValidation.setValidationMethod(TicketValidationMethod.QR_SCAN);
      invalidValidation.setStatus(TicketValidationStatusEnum.INVALID);
      return ticketValidationRepository.save(invalidValidation);
    }

    Ticket ticket = qrCodeOpt.get().getTicket();
    return validateTicket(ticket, TicketValidationMethod.QR_SCAN);
  }

  private TicketValidation validateTicket(Ticket ticket,
                                          TicketValidationMethod ticketValidationMethod) {
    TicketValidation ticketValidation = new TicketValidation();
    ticketValidation.setTicket(ticket);
    ticketValidation.setValidationMethod(ticketValidationMethod);

    // Check if ticket is already expired
    if (TicketStatusEnum.EXPIRED.equals(ticket.getStatus())) {
      ticketValidation.setStatus(TicketValidationStatusEnum.EXPIRED);
      return ticketValidationRepository.save(ticketValidation);
    }

    // Check if the event has already ended
    LocalDateTime eventEnd = ticket.getTicketType().getEvent().getEnd();
    if (eventEnd != null && LocalDateTime.now().isAfter(eventEnd)) {
      ticketValidation.setStatus(TicketValidationStatusEnum.EXPIRED);
      // Also mark the ticket as expired
      ticket.setStatus(TicketStatusEnum.EXPIRED);
      ticketRepository.save(ticket);
      return ticketValidationRepository.save(ticketValidation);
    }

    // Check if ticket was already used (has a valid validation or status is USED)
    if (TicketStatusEnum.USED.equals(ticket.getStatus())) {
      ticketValidation.setStatus(TicketValidationStatusEnum.ALREADY_USED);
      return ticketValidationRepository.save(ticketValidation);
    }

    // Check if ticket was already used (has a valid validation)
    TicketValidationStatusEnum ticketValidationStatus = ticket.getValidations().stream()
        .filter(v -> TicketValidationStatusEnum.VALID.equals(v.getStatus()))
        .findFirst()
        .map(v -> TicketValidationStatusEnum.ALREADY_USED)
        .orElse(TicketValidationStatusEnum.VALID);

    ticketValidation.setStatus(ticketValidationStatus);

    // If this is a valid scan (first successful validation), mark the ticket as USED
    if (TicketValidationStatusEnum.VALID.equals(ticketValidationStatus)) {
      ticket.setStatus(TicketStatusEnum.USED);
      ticketRepository.save(ticket);
    }

    return ticketValidationRepository.save(ticketValidation);
  }

  @Override
  public TicketValidation validateTicketManually(UUID ticketId) {
    Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
    
    // If ticket not found, return INVALID status
    if (ticketOpt.isEmpty()) {
      TicketValidation invalidValidation = new TicketValidation();
      invalidValidation.setValidationMethod(TicketValidationMethod.MANUAL);
      invalidValidation.setStatus(TicketValidationStatusEnum.INVALID);
      return ticketValidationRepository.save(invalidValidation);
    }
    
    return validateTicket(ticketOpt.get(), TicketValidationMethod.MANUAL);
  }
}
