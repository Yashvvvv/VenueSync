package com.fullstack.venuesync.tickets.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.events.domain.EventStatusEnum;
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

@ExtendWith(MockitoExtension.class)
class TicketTypeServiceImplTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private TicketTypeRepository ticketTypeRepository;

  @Mock
  private TicketRepository ticketRepository;

  @Mock
  private QrCodeService qrCodeService;

  @InjectMocks
  private TicketTypeServiceImpl ticketTypeService;

  private UUID userId;
  private UUID eventId;
  private UUID ticketTypeId;
  private UUID key;
  private User user;
  private Event event;
  private TicketType ticketType;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    eventId = UUID.randomUUID();
    ticketTypeId = UUID.randomUUID();
    key = UUID.randomUUID();

    user = new User();
    user.setId(userId);
    user.setName("Test User");
    user.setEmail("user@test.com");

    event = new Event();
    event.setId(eventId);
    event.setName("Test Event");
    event.setStatus(EventStatusEnum.PUBLISHED);
    event.setSalesStart(LocalDateTime.now().minusDays(1));
    event.setSalesEnd(LocalDateTime.now().plusDays(5));
    event.setEnd(LocalDateTime.now().plusDays(10));

    ticketType = new TicketType();
    ticketType.setId(ticketTypeId);
    ticketType.setName("General");
    ticketType.setPrice(50.0);
    ticketType.setTotalAvailable(100);
    ticketType.setEvent(event);
    event.setTicketTypes(new ArrayList<>(List.of(ticketType)));
  }

  private void givenTicketTypeInEvent() {
    when(ticketTypeRepository.findByIdAndEventIdWithLock(ticketTypeId, eventId)).thenReturn(Optional.of(ticketType));
  }

  private void givenNoPreviousPurchase() {
    when(ticketRepository.findByPurchaserIdAndIdempotencyKey(userId, key)).thenReturn(Optional.empty());
  }

  private void givenSaveWorks() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> {
      Ticket t = i.getArgument(0);
      t.setId(UUID.randomUUID());
      return t;
    });
  }

  private Ticket purchase() {
    return ticketTypeService.purchaseTicket(userId, eventId, ticketTypeId, key);
  }

  @Nested
  @DisplayName("purchaseTicket")
  class PurchaseTicketTests {

    @Test
    @DisplayName("should purchase ticket and store the idempotency key")
    void shouldPurchaseTicketSuccessfully() {
      givenTicketTypeInEvent();
      givenNoPreviousPurchase();
      givenSaveWorks();
      when(ticketRepository.countByTicketTypeId(ticketTypeId)).thenReturn(10);

      Ticket result = purchase();

      assertEquals(TicketStatusEnum.PURCHASED, result.getStatus());
      assertEquals(user, result.getPurchaser());
      assertEquals(ticketType, result.getTicketType());
      assertEquals(key, result.getIdempotencyKey());
      assertEquals(50.0, result.getPricePaid()); // fixed at purchase, whatever the type costs later
      verify(qrCodeService).generateQrCode(any(Ticket.class));
    }

    @Test
    @DisplayName("should return the existing ticket when the same key is replayed")
    void shouldReturnExistingTicketOnReplay() {
      Ticket existing = new Ticket();
      existing.setId(UUID.randomUUID());
      existing.setTicketType(ticketType);
      givenTicketTypeInEvent();
      when(ticketRepository.findByPurchaserIdAndIdempotencyKey(userId, key)).thenReturn(Optional.of(existing));

      assertSame(existing, purchase());
      verify(ticketRepository, never()).save(any());
      verify(qrCodeService, never()).generateQrCode(any());
    }

    @Test
    @DisplayName("should return the existing ticket on replay even after the ticket type sold out")
    void shouldReplayBeforeSoldOutCheck() {
      Ticket existing = new Ticket();
      existing.setTicketType(ticketType);
      ticketType.setTotalAvailable(1);
      givenTicketTypeInEvent();
      when(ticketRepository.findByPurchaserIdAndIdempotencyKey(userId, key)).thenReturn(Optional.of(existing));
      lenient().when(ticketRepository.countByTicketTypeId(ticketTypeId)).thenReturn(1); // the replayed ticket was the last one

      assertSame(existing, purchase());
    }

    @Test
    @DisplayName("should return the existing ticket on replay even after sales ended")
    void shouldReplayBeforeSalesCheck() {
      Ticket existing = new Ticket();
      existing.setTicketType(ticketType);
      event.setSalesEnd(LocalDateTime.now().minusMinutes(1));
      givenTicketTypeInEvent();
      when(ticketRepository.findByPurchaserIdAndIdempotencyKey(userId, key)).thenReturn(Optional.of(existing));

      assertSame(existing, purchase());
    }

    @Test
    @DisplayName("should reject a key already used for a different ticket type")
    void shouldRejectKeyReusedForOtherTicketType() {
      TicketType other = new TicketType();
      other.setId(UUID.randomUUID());
      Ticket existing = new Ticket();
      existing.setTicketType(other);
      givenTicketTypeInEvent();
      when(ticketRepository.findByPurchaserIdAndIdempotencyKey(userId, key)).thenReturn(Optional.of(existing));

      assertThrows(IdempotencyKeyReusedException.class, () -> purchase());
      verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw TicketTypeNotFoundException when the ticket type is not in this event")
    void shouldThrowWhenTicketTypeNotInEvent() {
      when(ticketTypeRepository.findByIdAndEventIdWithLock(ticketTypeId, eventId)).thenReturn(Optional.empty());

      assertThrows(TicketTypeNotFoundException.class, () -> purchase());
      verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw UserNotFoundException when user not found")
    void shouldThrowUserNotFoundWhenUserNotFound() {
      givenTicketTypeInEvent();
      givenNoPreviousPurchase();
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      assertThrows(UserNotFoundException.class, () -> purchase());
    }

    @Test
    @DisplayName("should throw SalesPeriodException(UPCOMING) when sales not started")
    void shouldThrowWhenSalesNotStarted() {
      event.setSalesStart(LocalDateTime.now().plusDays(1));
      givenTicketTypeInEvent();
      givenNoPreviousPurchase();
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      SalesPeriodException ex = assertThrows(SalesPeriodException.class, () -> purchase());
      assertEquals(SalesStatus.UPCOMING, ex.getSalesStatus());
    }

    @Test
    @DisplayName("should throw SalesPeriodException(ENDED) when sales ended")
    void shouldThrowWhenSalesEnded() {
      event.setSalesStart(LocalDateTime.now().minusDays(10));
      event.setSalesEnd(LocalDateTime.now().minusDays(1));
      givenTicketTypeInEvent();
      givenNoPreviousPurchase();
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      SalesPeriodException ex = assertThrows(SalesPeriodException.class, () -> purchase());
      assertEquals(SalesStatus.ENDED, ex.getSalesStatus());
    }

    @Test
    @DisplayName("should throw SalesPeriodException(ENDED) when the event has ended")
    void shouldThrowWhenEventEnded() {
      event.setSalesStart(null);
      event.setSalesEnd(null);
      event.setEnd(LocalDateTime.now().minusDays(1));
      givenTicketTypeInEvent();
      givenNoPreviousPurchase();
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      SalesPeriodException ex = assertThrows(SalesPeriodException.class, () -> purchase());
      assertEquals(SalesStatus.ENDED, ex.getSalesStatus());
    }

    @Test
    @DisplayName("should throw TicketsSoldOutException when sold out")
    void shouldThrowTicketsSoldOutWhenSoldOut() {
      ticketType.setTotalAvailable(10);
      givenTicketTypeInEvent();
      givenNoPreviousPurchase();
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(ticketRepository.countByTicketTypeId(ticketTypeId)).thenReturn(10);

      assertThrows(TicketsSoldOutException.class, () -> purchase());
    }

    @Test
    @DisplayName("should allow purchase when totalAvailable is null (unlimited)")
    void shouldAllowPurchaseWhenUnlimited() {
      ticketType.setTotalAvailable(null);
      givenTicketTypeInEvent();
      givenNoPreviousPurchase();
      givenSaveWorks();
      when(ticketRepository.countByTicketTypeId(ticketTypeId)).thenReturn(1000);

      assertNotNull(purchase());
    }
  }

  @Nested
  @DisplayName("soldOutTicketTypeIds")
  class SoldOutTests {

    @Test
    @DisplayName("should report only the ticket types with no tickets left")
    void shouldReportSoldOutTypes() {
      TicketType unlimited = new TicketType();
      unlimited.setId(UUID.randomUUID());
      unlimited.setTotalAvailable(null);
      TicketType unsold = new TicketType();
      unsold.setId(UUID.randomUUID());
      unsold.setTotalAvailable(5);
      ticketType.setTotalAvailable(2);
      event.setTicketTypes(new ArrayList<>(List.of(ticketType, unlimited, unsold)));
      when(ticketRepository.countSoldByTicketTypeForEvent(eventId)).thenReturn(List.of(
          new Object[]{ticketTypeId, 2L},
          new Object[]{unlimited.getId(), 5000L}));

      assertEquals(Set.of(ticketTypeId), ticketTypeService.soldOutTicketTypeIds(event));
    }
  }
}
