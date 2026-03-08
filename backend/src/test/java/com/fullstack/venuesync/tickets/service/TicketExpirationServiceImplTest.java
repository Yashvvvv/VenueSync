package com.fullstack.venuesync.tickets.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fullstack.venuesync.tickets.domain.TicketStatusEnum;
import com.fullstack.venuesync.tickets.repository.TicketRepository;

@ExtendWith(MockitoExtension.class)
class TicketExpirationServiceImplTest {

  @Mock
  private TicketRepository ticketRepository;

  @InjectMocks
  private TicketExpirationServiceImpl ticketExpirationService;

  @Test
  @DisplayName("should expire tickets for ended events and return count")
  void shouldExpireTicketsForEndedEvents() {
    when(ticketRepository.expireTicketsForEndedEvents(
        eq(TicketStatusEnum.EXPIRED),
        eq(TicketStatusEnum.PURCHASED),
        any(LocalDateTime.class)
    )).thenReturn(5);

    int result = ticketExpirationService.expireTicketsForEndedEvents();

    assertEquals(5, result);
    verify(ticketRepository).expireTicketsForEndedEvents(
        eq(TicketStatusEnum.EXPIRED),
        eq(TicketStatusEnum.PURCHASED),
        any(LocalDateTime.class)
    );
  }

  @Test
  @DisplayName("should return zero when no tickets to expire")
  void shouldReturnZeroWhenNoTicketsToExpire() {
    when(ticketRepository.expireTicketsForEndedEvents(
        eq(TicketStatusEnum.EXPIRED),
        eq(TicketStatusEnum.PURCHASED),
        any(LocalDateTime.class)
    )).thenReturn(0);

    int result = ticketExpirationService.expireTicketsForEndedEvents();

    assertEquals(0, result);
  }
}
