package com.fullstack.venuesync.validation.service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.fullstack.venuesync.validation.domain.QrCode;
import com.fullstack.venuesync.validation.domain.QrCodeStatusEnum;
import com.fullstack.venuesync.tickets.domain.Ticket;
import com.fullstack.venuesync.tickets.domain.TicketStatusEnum;
import com.fullstack.venuesync.validation.domain.TicketValidation;
import com.fullstack.venuesync.validation.domain.TicketValidationMethod;
import com.fullstack.venuesync.validation.domain.TicketValidationStatusEnum;
import com.fullstack.venuesync.validation.repository.QrCodeRepository;
import com.fullstack.venuesync.tickets.repository.TicketRepository;
import com.fullstack.venuesync.validation.repository.TicketValidationRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketValidationServiceImpl implements TicketValidationService {

  private final QrCodeRepository qrCodeRepository;
  private final TicketValidationRepository ticketValidationRepository;
  private final TicketRepository ticketRepository;

  @Override
  public TicketValidation validateTicketByQrCode(UUID qrCodeId, @Nullable UUID idempotencyKey) {
    return replayOrRecord(idempotencyKey, () -> {
      Optional<QrCode> qrCodeOpt = qrCodeRepository.findByIdAndStatus(qrCodeId, QrCodeStatusEnum.ACTIVE);

      // If QR code not found or inactive, return INVALID status
      if (qrCodeOpt.isEmpty()) {
        return invalid(TicketValidationMethod.QR_SCAN);
      }
      return validateTicket(qrCodeOpt.get().getTicket(), TicketValidationMethod.QR_SCAN);
    });
  }

  @Override
  public TicketValidation validateTicketManually(UUID ticketId, @Nullable UUID idempotencyKey) {
    return replayOrRecord(idempotencyKey, () -> {
      Optional<Ticket> ticketOpt = ticketRepository.findById(Objects.requireNonNull(ticketId));

      // If ticket not found, return INVALID status
      if (ticketOpt.isEmpty()) {
        return invalid(TicketValidationMethod.MANUAL);
      }
      return validateTicket(ticketOpt.get(), TicketValidationMethod.MANUAL);
    });
  }

  /**
   * A scan whose response was lost (venue Wi-Fi) is retried with the same key and must get the FIRST answer:
   * re-running it would say ALREADY_USED about the ticket this very scan just admitted. A key identifies one
   * scan; reusing it for a different scan is a client bug and gets the first scan's answer.
   */
  private TicketValidation replayOrRecord(@Nullable UUID idempotencyKey, Supplier<TicketValidation> validation) {
    if (idempotencyKey != null) {
      Optional<TicketValidation> previous = ticketValidationRepository.findByIdempotencyKey(idempotencyKey);
      if (previous.isPresent()) {
        return previous.get();
      }
    }
    TicketValidation result = validation.get();
    result.setIdempotencyKey(idempotencyKey);
    return ticketValidationRepository.save(result);
  }

  private TicketValidation invalid(TicketValidationMethod method) {
    TicketValidation invalidValidation = new TicketValidation();
    invalidValidation.setValidationMethod(method);
    invalidValidation.setStatus(TicketValidationStatusEnum.INVALID);
    return invalidValidation;
  }

  /** Decides the outcome (admitting the ticket if VALID); the caller stores the validation. */
  private TicketValidation validateTicket(Ticket ticket, TicketValidationMethod ticketValidationMethod) {
    TicketValidation ticketValidation = new TicketValidation();
    ticketValidation.setTicket(ticket);
    ticketValidation.setValidationMethod(ticketValidationMethod);

    // Check if ticket is already expired
    if (TicketStatusEnum.EXPIRED.equals(ticket.getStatus())) {
      ticketValidation.setStatus(TicketValidationStatusEnum.EXPIRED);
      return ticketValidation;
    }

    // Check if the event has already ended
    LocalDateTime eventEnd = ticket.getTicketType().getEvent().getEnd();
    if (eventEnd != null && LocalDateTime.now().isAfter(eventEnd)) {
      ticketValidation.setStatus(TicketValidationStatusEnum.EXPIRED);
      // Also mark the ticket as expired
      ticket.setStatus(TicketStatusEnum.EXPIRED);
      ticketRepository.save(ticket);
      return ticketValidation;
    }

    // A cancelled (refunded) ticket must never get in.
    if (TicketStatusEnum.CANCELLED.equals(ticket.getStatus())) {
      ticketValidation.setStatus(TicketValidationStatusEnum.INVALID);
      return ticketValidation;
    }

    // The status column is the single source of truth, and check-and-set is one atomic UPDATE:
    // of two simultaneous scans of one ticket, only one can be VALID.
    boolean admitted = ticketRepository.markUsed(
        ticket.getId(), TicketStatusEnum.PURCHASED, TicketStatusEnum.USED, LocalDateTime.now()) == 1;
    ticketValidation.setStatus(admitted
        ? TicketValidationStatusEnum.VALID
        : TicketValidationStatusEnum.ALREADY_USED);
    return ticketValidation;
  }
}
