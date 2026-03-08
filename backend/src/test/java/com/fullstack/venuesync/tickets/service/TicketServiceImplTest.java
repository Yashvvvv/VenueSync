package com.fullstack.venuesync.tickets.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.fullstack.venuesync.tickets.domain.Ticket;
import com.fullstack.venuesync.tickets.domain.TicketStatusEnum;
import com.fullstack.venuesync.tickets.repository.TicketRepository;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

  @Mock
  private TicketRepository ticketRepository;

  @InjectMocks
  private TicketServiceImpl ticketService;

  private UUID userId;
  private UUID ticketId;
  private Ticket ticket;
  private Pageable pageable;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    ticketId = UUID.randomUUID();
    pageable = PageRequest.of(0, 10);

    ticket = new Ticket();
    ticket.setId(ticketId);
    ticket.setStatus(TicketStatusEnum.PURCHASED);
  }

  @Test
  @DisplayName("should list all tickets for user")
  void shouldListAllTicketsForUser() {
    Page<Ticket> expectedPage = new PageImpl<>(List.of(ticket));
    when(ticketRepository.findByPurchaserId(userId, pageable)).thenReturn(expectedPage);

    Page<Ticket> result = ticketService.listTicketsForUser(userId, pageable);

    assertEquals(1, result.getTotalElements());
    assertEquals(ticket, result.getContent().get(0));
  }

  @Test
  @DisplayName("should list active tickets for user")
  void shouldListActiveTicketsForUser() {
    Page<Ticket> expectedPage = new PageImpl<>(List.of(ticket));
    when(ticketRepository.findActiveTicketsByPurchaserId(
        eq(userId), eq(TicketStatusEnum.PURCHASED), any(LocalDateTime.class), eq(pageable)
    )).thenReturn(expectedPage);

    Page<Ticket> result = ticketService.listActiveTicketsForUser(userId, pageable);

    assertEquals(1, result.getTotalElements());
  }

  @Test
  @DisplayName("should list past tickets for user")
  void shouldListPastTicketsForUser() {
    ticket.setStatus(TicketStatusEnum.USED);
    Page<Ticket> expectedPage = new PageImpl<>(List.of(ticket));

    when(ticketRepository.findPastTicketsByPurchaserId(
        eq(userId), anyList(), any(LocalDateTime.class), eq(pageable)
    )).thenReturn(expectedPage);

    Page<Ticket> result = ticketService.listPastTicketsForUser(userId, pageable);

    assertEquals(1, result.getTotalElements());
  }

  @Test
  @DisplayName("should get ticket for user")
  void shouldGetTicketForUser() {
    when(ticketRepository.findByIdAndPurchaserId(ticketId, userId))
        .thenReturn(Optional.of(ticket));

    Optional<Ticket> result = ticketService.getTicketForUser(userId, ticketId);

    assertTrue(result.isPresent());
    assertEquals(ticket, result.get());
  }

  @Test
  @DisplayName("should return empty when ticket not found for user")
  void shouldReturnEmptyWhenTicketNotFound() {
    when(ticketRepository.findByIdAndPurchaserId(ticketId, userId))
        .thenReturn(Optional.empty());

    Optional<Ticket> result = ticketService.getTicketForUser(userId, ticketId);

    assertFalse(result.isPresent());
  }
}
