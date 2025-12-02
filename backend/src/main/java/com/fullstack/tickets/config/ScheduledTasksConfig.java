package com.fullstack.tickets.config;

import com.fullstack.tickets.services.EventStatusService;
import com.fullstack.tickets.services.TicketExpirationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuration class for scheduled tasks.
 * Handles automatic ticket expiration and event status updates.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasksConfig {

  private final TicketExpirationService ticketExpirationService;
  private final EventStatusService eventStatusService;

  /**
   * Runs every hour to check for and expire tickets for events that have ended.
   * Also runs 10 seconds after application startup.
   */
  @Scheduled(fixedRate = 3600000, initialDelay = 10000) // Every hour, 10 seconds after startup
  public void expireTicketsTask() {
    log.debug("Running scheduled ticket expiration task...");
    try {
      ticketExpirationService.expireTicketsForEndedEvents();
    } catch (Exception e) {
      log.error("Error during scheduled ticket expiration", e);
    }
  }

  /**
   * Runs every hour to automatically mark PUBLISHED events as COMPLETED
   * when their event_end date has passed.
   * Also runs 10 seconds after application startup.
   */
  @Scheduled(fixedRate = 3600000, initialDelay = 10000) // Every hour, 10 seconds after startup
  public void completeEndedEventsTask() {
    log.debug("Running scheduled event completion task...");
    try {
      eventStatusService.completeEndedEvents();
    } catch (Exception e) {
      log.error("Error during scheduled event completion", e);
    }
  }
}
