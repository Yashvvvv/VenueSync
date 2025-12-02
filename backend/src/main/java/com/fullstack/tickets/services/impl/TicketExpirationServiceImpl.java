package com.fullstack.tickets.services.impl;

import com.fullstack.tickets.domain.entities.TicketStatusEnum;
import com.fullstack.tickets.repositories.TicketRepository;
import com.fullstack.tickets.services.TicketExpirationService;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketExpirationServiceImpl implements TicketExpirationService {

  private final TicketRepository ticketRepository;

  @Override
  @Transactional
  public int expireTicketsForEndedEvents() {
    LocalDateTime now = LocalDateTime.now();
    int expiredCount = ticketRepository.expireTicketsForEndedEvents(
        TicketStatusEnum.EXPIRED,
        TicketStatusEnum.PURCHASED,
        now
    );
    
    if (expiredCount > 0) {
      log.info("Expired {} tickets for events that have ended", expiredCount);
    }
    
    return expiredCount;
  }
}
