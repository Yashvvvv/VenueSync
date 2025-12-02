package com.fullstack.tickets.services.impl;

import com.fullstack.tickets.domain.entities.EventStatusEnum;
import com.fullstack.tickets.repositories.EventRepository;
import com.fullstack.tickets.services.EventStatusService;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementation of EventStatusService.
 * Handles automatic event status transitions based on dates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventStatusServiceImpl implements EventStatusService {

  private final EventRepository eventRepository;

  @Override
  @Transactional
  public int completeEndedEvents() {
    LocalDateTime now = LocalDateTime.now();
    log.debug("Checking for PUBLISHED events that have ended (event_end < {})", now);
    
    int completedCount = eventRepository.completeEndedEvents(
        EventStatusEnum.COMPLETED,
        EventStatusEnum.PUBLISHED,
        now
    );
    
    if (completedCount > 0) {
      log.info("Automatically completed {} events that have ended", completedCount);
    }
    
    return completedCount;
  }
}
