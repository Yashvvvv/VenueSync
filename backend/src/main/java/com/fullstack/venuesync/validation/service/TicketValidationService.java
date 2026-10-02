package com.fullstack.venuesync.validation.service;

import java.util.UUID;
import org.springframework.lang.Nullable;

import com.fullstack.venuesync.validation.domain.TicketValidation;

public interface TicketValidationService {

  /**
   * Validates a ticket by scanning its QR code.
   * Looks up the QR code by ID, checks ticket status, and marks the ticket as USED if valid.
   *
   * @param qrCodeId the UUID from the scanned QR code
   * @param userId the scanning user; must be organizer or staff of the ticket's event (else NotEventStaffException)
   * @param idempotencyKey optional; a retry with the same key returns the first answer instead of re-validating
   * @param eventId optional; a ticket for another event is WRONG_EVENT, not stored, ticket untouched
   * @return a TicketValidation entity with the validation result (VALID, INVALID, EXPIRED, or ALREADY_USED)
   */
  TicketValidation validateTicketByQrCode(UUID qrCodeId, UUID userId, @Nullable UUID idempotencyKey, @Nullable UUID eventId);

  /**
   * Validates a ticket manually by ticket ID.
   * Looks up the ticket directly, checks status, and marks it as USED if valid.
   *
   * @param ticketId the UUID of the ticket to validate
   * @param userId the scanning user; must be organizer or staff of the ticket's event (else NotEventStaffException)
   * @param idempotencyKey optional; a retry with the same key returns the first answer instead of re-validating
   * @param eventId optional; a ticket for another event is WRONG_EVENT, not stored, ticket untouched
   * @return a TicketValidation entity with the validation result (VALID, INVALID, EXPIRED, or ALREADY_USED)
   */
  TicketValidation validateTicketManually(UUID ticketId, UUID userId, @Nullable UUID idempotencyKey, @Nullable UUID eventId);
}
