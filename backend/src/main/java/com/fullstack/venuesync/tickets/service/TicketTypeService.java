package com.fullstack.venuesync.tickets.service;

import java.util.UUID;

import com.fullstack.venuesync.tickets.domain.Ticket;

public interface TicketTypeService {

  /**
   * Purchases a ticket for a user with pessimistic locking to prevent overselling.
   * Validates the sales period, checks ticket availability, creates the ticket,
   * and generates an associated QR code.
   *
   * @param userId the UUID of the purchasing user
   * @param ticketTypeId the UUID of the ticket type to purchase
   * @return the newly created Ticket entity
   * @throws com.fullstack.venuesync.shared.exceptions.UserNotFoundException if the user is not found
   * @throws com.fullstack.venuesync.tickets.exception.TicketTypeNotFoundException if the ticket type is not found
   * @throws com.fullstack.venuesync.events.exception.SalesPeriodException if sales haven't started, have ended, or the event has ended
   * @throws com.fullstack.venuesync.tickets.exception.TicketsSoldOutException if no tickets are available
   */
  Ticket purchaseTicket(UUID userId, UUID ticketTypeId);
}
