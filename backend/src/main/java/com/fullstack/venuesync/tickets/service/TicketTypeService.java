package com.fullstack.venuesync.tickets.service;

import java.util.Set;
import java.util.UUID;
import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.tickets.domain.Ticket;

public interface TicketTypeService {

  /**
   * Purchases one ticket, idempotently: the same user, key and ticket type always yield the same
   * ticket, so a client may safely retry after a timeout or dropped connection.
   *
   * @param idempotencyKey client-generated, one per purchase attempt
   * @return the new ticket, or the one previously created with this key
   * @throws com.fullstack.venuesync.tickets.exception.TicketTypeNotFoundException if the ticket type doesn't exist in this event
   * @throws com.fullstack.venuesync.tickets.exception.IdempotencyKeyReusedException if the key was used for another ticket type
   * @throws com.fullstack.venuesync.events.exception.SalesPeriodException if the event isn't on sale
   * @throws com.fullstack.venuesync.tickets.exception.TicketsSoldOutException if no tickets are left
   */
  Ticket purchaseTicket(UUID userId, UUID eventId, UUID ticketTypeId, UUID idempotencyKey);

  /** Ids of the event's ticket types that have no tickets left. */
  Set<UUID> soldOutTicketTypeIds(Event event);
}
