package com.fullstack.venuesync.tickets.service;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.fullstack.venuesync.tickets.domain.Ticket;

public interface TicketService {

  /**
   * Lists all tickets for a user with pagination.
   *
   * @param userId the UUID of the ticket purchaser
   * @param pageable pagination parameters
   * @return a paginated list of all tickets for the user
   */
  Page<Ticket> listTicketsForUser(UUID userId, Pageable pageable);
  
  /**
   * Lists active tickets for a user (upcoming events with PURCHASED status).
   * Active tickets are those where the event has not yet ended.
   *
   * @param userId the UUID of the ticket purchaser
   * @param pageable pagination parameters
   * @return a paginated list of active tickets ordered by event start date ascending
   */
  Page<Ticket> listActiveTicketsForUser(UUID userId, Pageable pageable);
  
  /**
   * Lists past tickets for a user (USED, EXPIRED, CANCELLED, or events that have ended).
   *
   * @param userId the UUID of the ticket purchaser
   * @param pageable pagination parameters
   * @return a paginated list of past tickets ordered by event start date descending
   */
  Page<Ticket> listPastTicketsForUser(UUID userId, Pageable pageable);
  
  /**
   * Retrieves a specific ticket for a user.
   *
   * @param userId the UUID of the ticket purchaser
   * @param ticketId the UUID of the ticket
   * @return an Optional containing the ticket if found and owned by the user
   */
  Optional<Ticket> getTicketForUser(UUID userId, UUID ticketId);
}
