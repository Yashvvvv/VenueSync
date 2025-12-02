package com.fullstack.tickets.services;

/**
 * Service for managing automatic event status transitions.
 * Handles automatic completion of events when their end date has passed.
 */
public interface EventStatusService {

  /**
   * Automatically marks PUBLISHED events as COMPLETED when their event_end date has passed.
   * This should be called periodically by a scheduled task.
   * 
   * @return the number of events that were marked as completed
   */
  int completeEndedEvents();
}
