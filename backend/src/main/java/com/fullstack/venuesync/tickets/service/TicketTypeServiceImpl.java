package com.fullstack.venuesync.tickets.service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.events.exception.SalesPeriodException;
import com.fullstack.venuesync.shared.domain.User;
import com.fullstack.venuesync.shared.domain.UserRepository;
import com.fullstack.venuesync.shared.exceptions.UserNotFoundException;
import com.fullstack.venuesync.tickets.domain.Ticket;
import com.fullstack.venuesync.tickets.domain.TicketStatusEnum;
import com.fullstack.venuesync.tickets.domain.TicketType;
import com.fullstack.venuesync.tickets.exception.TicketTypeNotFoundException;
import com.fullstack.venuesync.tickets.exception.TicketsSoldOutException;
import com.fullstack.venuesync.tickets.repository.TicketRepository;
import com.fullstack.venuesync.tickets.repository.TicketTypeRepository;
import com.fullstack.venuesync.validation.service.QrCodeService;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketTypeServiceImpl implements TicketTypeService {

  private final UserRepository userRepository;
  private final TicketTypeRepository ticketTypeRepository;
  private final TicketRepository ticketRepository;
  private final QrCodeService qrCodeService;

  @Override
  @Transactional
  public Ticket purchaseTicket(UUID userId, UUID ticketTypeId) {
    User user = userRepository.findById(Objects.requireNonNull(userId)).orElseThrow(() -> new UserNotFoundException(
        String.format("User with ID %s was not found", userId)
    ));

    TicketType ticketType = ticketTypeRepository.findByIdWithLock(ticketTypeId)
        .orElseThrow(() -> new TicketTypeNotFoundException(
            String.format("Ticket type with ID %s was not found", ticketTypeId)
        ));

    // Validate sales period - use system local time to match wall clock times
    // Event times are stored as "wall clock" times without timezone info
    Event event = ticketType.getEvent();
    LocalDateTime now = LocalDateTime.now();

    log.info("Sales validation - Now: {}, SalesStart: {}, SalesEnd: {}, EventEnd: {}", 
             now, event.getSalesStart(), event.getSalesEnd(), event.getEnd());

    if (event.getSalesStart() != null && now.isBefore(event.getSalesStart())) {
      log.warn("Sales not started - Now {} is before SalesStart {}", now, event.getSalesStart());
      throw new SalesPeriodException("Ticket sales have not started yet");
    }

    if (event.getSalesEnd() != null && now.isAfter(event.getSalesEnd())) {
      throw new SalesPeriodException("Ticket sales have ended");
    }

    // Also check if event has already ended
    if (event.getEnd() != null && now.isAfter(event.getEnd())) {
      throw new SalesPeriodException("This event has already ended");
    }

    int purchasedTickets = ticketRepository.countByTicketTypeId(ticketType.getId());
    Integer totalAvailable = ticketType.getTotalAvailable();

    // If totalAvailable is null, it means unlimited tickets
    if(totalAvailable != null && purchasedTickets + 1 > totalAvailable) {
      throw new TicketsSoldOutException();
    }

    Ticket ticket = new Ticket();
    ticket.setStatus(TicketStatusEnum.PURCHASED);
    ticket.setTicketType(ticketType);
    ticket.setPurchaser(user);

    Ticket savedTicket = ticketRepository.save(ticket);
    qrCodeService.generateQrCode(savedTicket);

    return ticketRepository.save(savedTicket);
  }
}
