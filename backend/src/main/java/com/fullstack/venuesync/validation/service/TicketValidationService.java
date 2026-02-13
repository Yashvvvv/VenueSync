package com.fullstack.venuesync.validation.service;

import java.util.UUID;

import com.fullstack.venuesync.validation.domain.TicketValidation;

public interface TicketValidationService {
  TicketValidation validateTicketByQrCode(UUID qrCodeId);
  TicketValidation validateTicketManually(UUID ticketId);
}
