package com.fullstack.venuesync.validation.domain;

public enum TicketValidationStatusEnum {
  VALID, INVALID, EXPIRED, ALREADY_USED,
  /**
   * Response-only: the ticket belongs to another event than the one the scanner works. NEVER store it:
   * Hibernate created ticket_validations_status_check with the four values above, and ddl-auto=update
   * does not alter CHECK constraints, so saving this would fail on every existing database.
   */
  WRONG_EVENT
}
