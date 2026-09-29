package com.fullstack.venuesync.tickets.service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.events.domain.SalesStatus;
import com.fullstack.venuesync.events.exception.SalesPeriodException;
import com.fullstack.venuesync.shared.domain.User;
import com.fullstack.venuesync.shared.domain.UserRepository;
import com.fullstack.venuesync.shared.exceptions.UserNotFoundException;
import com.fullstack.venuesync.tickets.domain.Ticket;
import com.fullstack.venuesync.tickets.domain.TicketStatusEnum;
import com.fullstack.venuesync.tickets.domain.TicketType;
import com.fullstack.venuesync.tickets.exception.IdempotencyKeyReusedException;
import com.fullstack.venuesync.tickets.exception.TicketTypeNotFoundException;
import com.fullstack.venuesync.tickets.exception.TicketsSoldOutException;
import com.fullstack.venuesync.tickets.repository.TicketRepository;
import com.fullstack.venuesync.tickets.repository.TicketTypeRepository;
import com.fullstack.venuesync.validation.service.QrCodeService;

@Service
@RequiredArgsConstructor
public class TicketTypeServiceImpl implements TicketTypeService {

  private final UserRepository userRepository;
  private final TicketTypeRepository ticketTypeRepository;
  private final TicketRepository ticketRepository;
  private final QrCodeService qrCodeService;

  @Override
  @Transactional
  public Ticket purchaseTicket(UUID userId, UUID eventId, UUID ticketTypeId, UUID idempotencyKey) {
    // Row lock first: a concurrent retry with the same key blocks here until the first
    // purchase commits, then finds its ticket below instead of creating a second one.
    TicketType ticketType = ticketTypeRepository.findByIdAndEventIdWithLock(ticketTypeId, eventId)
        .orElseThrow(() -> new TicketTypeNotFoundException(
            String.format("Ticket type %s was not found in event %s", ticketTypeId, eventId)
        ));

    // Replay check BEFORE the sales and sold-out checks: retrying the purchase that took the
    // last ticket must return that ticket, not "sold out".
    Optional<Ticket> previous = ticketRepository.findByPurchaserIdAndIdempotencyKey(userId, idempotencyKey);
    if (previous.isPresent()) {
      if (!previous.get().getTicketType().getId().equals(ticketTypeId)) {
        throw new IdempotencyKeyReusedException();
      }
      return previous.get();
    }

    User user = userRepository.findById(Objects.requireNonNull(userId)).orElseThrow(() -> new UserNotFoundException(
        String.format("User with ID %s was not found", userId)
    ));

    // ponytail: server wall clock vs zone-less event times — correct only while organizers and
    // the server share a time zone; fixed by moving events to instants + an IANA zone.
    SalesStatus salesStatus = ticketType.getEvent().salesStatusAt(LocalDateTime.now());
    if (salesStatus != SalesStatus.ON_SALE) {
      throw new SalesPeriodException(salesStatus);
    }

    if (ticketType.isSoldOut(ticketRepository.countByTicketTypeId(ticketType.getId()))) {
      throw new TicketsSoldOutException();
    }

    Ticket ticket = new Ticket();
    ticket.setStatus(TicketStatusEnum.PURCHASED);
    ticket.setTicketType(ticketType);
    ticket.setPurchaser(user);
    ticket.setIdempotencyKey(idempotencyKey);

    Ticket savedTicket = ticketRepository.save(ticket);
    qrCodeService.generateQrCode(savedTicket);

    return ticketRepository.save(savedTicket);
  }

  @Override
  @Transactional
  public Set<UUID> soldOutTicketTypeIds(Event event) {
    Map<UUID, Long> sold = ticketRepository.countSoldByTicketTypeForEvent(event.getId()).stream()
        .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
    return event.getTicketTypes().stream()
        .filter(ticketType -> ticketType.isSoldOut(sold.getOrDefault(ticketType.getId(), 0L)))
        .map(TicketType::getId)
        .collect(Collectors.toSet());
  }
}
