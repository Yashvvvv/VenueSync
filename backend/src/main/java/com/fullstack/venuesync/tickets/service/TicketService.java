package com.fullstack.venuesync.tickets.service;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.fullstack.venuesync.tickets.domain.Ticket;

public interface TicketService {
  Page<Ticket> listTicketsForUser(UUID userId, Pageable pageable);
  
  /**
   * List active tickets for a user (upcoming events, not yet used).
   */
  Page<Ticket> listActiveTicketsForUser(UUID userId, Pageable pageable);
  
  /**
   * List past tickets for a user (used, expired, or events that have ended).
   */
  Page<Ticket> listPastTicketsForUser(UUID userId, Pageable pageable);
  
  Optional<Ticket> getTicketForUser(UUID userId, UUID ticketId);
}
