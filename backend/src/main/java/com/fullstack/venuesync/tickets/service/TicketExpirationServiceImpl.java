package com.fullstack.venuesync.tickets.service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.fullstack.venuesync.tickets.domain.TicketStatusEnum;
import com.fullstack.venuesync.tickets.repository.TicketRepository;

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
