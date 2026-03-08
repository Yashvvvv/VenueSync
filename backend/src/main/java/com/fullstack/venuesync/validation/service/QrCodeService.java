package com.fullstack.venuesync.validation.service;

import java.util.UUID;

import com.fullstack.venuesync.validation.domain.QrCode;
import com.fullstack.venuesync.tickets.domain.Ticket;

public interface QrCodeService {

  /**
   * Generates a QR code for a purchased ticket.
   * Creates a unique UUID for the QR code, encodes it as a PNG image,
   * stores the Base64-encoded image, and saves the QR code entity.
   *
   * @param ticket the Ticket entity to generate a QR code for
   * @return the saved QrCode entity containing the Base64-encoded image
   * @throws com.fullstack.venuesync.validation.exception.QrCodeGenerationException if QR code generation fails
   */
  QrCode generateQrCode(Ticket ticket);

  /**
   * Retrieves the QR code image bytes for a specific ticket owned by a user.
   *
   * @param userId the UUID of the ticket owner
   * @param ticketId the UUID of the ticket
   * @return the QR code image as a byte array (PNG format)
   * @throws com.fullstack.venuesync.validation.exception.QrCodeNotFoundException if no QR code is found
   */
  byte[] getQrCodeImageForUserAndTicket(UUID userId, UUID ticketId);
}
