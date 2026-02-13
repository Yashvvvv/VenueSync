package com.fullstack.venuesync.tickets.service;

import java.util.UUID;

import com.fullstack.venuesync.tickets.domain.Ticket;

public interface TicketTypeService {
  Ticket purchaseTicket(UUID userId, UUID ticketTypeId);
}
