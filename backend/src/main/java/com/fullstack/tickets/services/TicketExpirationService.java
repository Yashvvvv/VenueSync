package com.fullstack.tickets.services;

/**
 * Service interface for handling ticket expiration logic.
 */
public interface TicketExpirationService {
  
  /**
   * Expires all tickets for events that have ended.
   * This marks tickets with status PURCHASED as EXPIRED if the event end date has passed.
   * 
   * @return the number of tickets that were expired
   */
  int expireTicketsForEndedEvents();
}
