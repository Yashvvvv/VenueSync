package com.fullstack.venuesync.validation.service;

import java.util.UUID;

import com.fullstack.venuesync.validation.domain.QrCode;
import com.fullstack.venuesync.tickets.domain.Ticket;

public interface QrCodeService {

  QrCode generateQrCode(Ticket ticket);

  byte[] getQrCodeImageForUserAndTicket(UUID userId, UUID ticketId);
}
