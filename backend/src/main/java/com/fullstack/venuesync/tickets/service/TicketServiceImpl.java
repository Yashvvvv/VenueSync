package com.fullstack.venuesync.tickets.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.fullstack.venuesync.tickets.domain.Ticket;
import com.fullstack.venuesync.tickets.domain.TicketStatusEnum;
import com.fullstack.venuesync.tickets.repository.TicketRepository;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

  private final TicketRepository ticketRepository;

  @Override
  public Page<Ticket> listTicketsForUser(UUID userId, Pageable pageable) {
    return ticketRepository.findByPurchaserId(userId, pageable);
  }

  @Override
  public Page<Ticket> listActiveTicketsForUser(UUID userId, Pageable pageable) {
    return ticketRepository.findActiveTicketsByPurchaserId(
        userId,
        TicketStatusEnum.PURCHASED,
        LocalDateTime.now(),
        pageable
    );
  }

  @Override
  public Page<Ticket> listPastTicketsForUser(UUID userId, Pageable pageable) {
    List<TicketStatusEnum> pastStatuses = List.of(
        TicketStatusEnum.USED,
        TicketStatusEnum.EXPIRED,
        TicketStatusEnum.CANCELLED
    );
    return ticketRepository.findPastTicketsByPurchaserId(
        userId,
        pastStatuses,
        LocalDateTime.now(),
        pageable
    );
  }

  @Override
  public Optional<Ticket> getTicketForUser(UUID userId, UUID ticketId) {
    return ticketRepository.findByIdAndPurchaserId(ticketId, userId);
  }
}
