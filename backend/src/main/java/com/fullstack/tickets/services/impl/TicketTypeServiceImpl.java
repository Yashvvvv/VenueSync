package com.fullstack.tickets.services.impl;

import com.fullstack.tickets.domain.entities.Event;
import com.fullstack.tickets.domain.entities.Ticket;
import com.fullstack.tickets.domain.entities.TicketStatusEnum;
import com.fullstack.tickets.domain.entities.TicketType;
import com.fullstack.tickets.domain.entities.User;
import com.fullstack.tickets.exceptions.SalesPeriodException;
import com.fullstack.tickets.exceptions.TicketTypeNotFoundException;
import com.fullstack.tickets.exceptions.TicketsSoldOutException;
import com.fullstack.tickets.exceptions.UserNotFoundException;
import com.fullstack.tickets.repositories.TicketRepository;
import com.fullstack.tickets.repositories.TicketTypeRepository;
import com.fullstack.tickets.repositories.UserRepository;
import com.fullstack.tickets.services.QrCodeService;
import com.fullstack.tickets.services.TicketTypeService;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(
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
