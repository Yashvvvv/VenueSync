package com.fullstack.venuesync.tickets.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
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
  private UUID ticketTypeId;
  private User user;
  private Event event;
  private TicketType ticketType;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    ticketTypeId = UUID.randomUUID();

    user = new User();
    user.setId(userId);
    user.setName("Test User");
    user.setEmail("user@test.com");

    event = new Event();
    event.setId(UUID.randomUUID());
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
  }

  @Nested
  @DisplayName("purchaseTicket")
  class PurchaseTicketTests {

    @Test
    @DisplayName("should purchase ticket successfully")
    void shouldPurchaseTicketSuccessfully() {
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(ticketTypeRepository.findByIdWithLock(ticketTypeId)).thenReturn(Optional.of(ticketType));
      when(ticketRepository.countByTicketTypeId(ticketTypeId)).thenReturn(10);
      when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> {
        Ticket t = i.getArgument(0);
        t.setId(UUID.randomUUID());
        return t;
      });

      Ticket result = ticketTypeService.purchaseTicket(userId, ticketTypeId);

      assertNotNull(result);
      assertEquals(TicketStatusEnum.PURCHASED, result.getStatus());
      assertEquals(user, result.getPurchaser());
      assertEquals(ticketType, result.getTicketType());
      verify(qrCodeService).generateQrCode(any(Ticket.class));
    }

    @Test
    @DisplayName("should throw UserNotFoundException when user not found")
    void shouldThrowUserNotFoundWhenUserNotFound() {
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      assertThrows(UserNotFoundException.class,
          () -> ticketTypeService.purchaseTicket(userId, ticketTypeId));
    }

    @Test
    @DisplayName("should throw TicketTypeNotFoundException when ticket type not found")
    void shouldThrowTicketTypeNotFoundWhenNotFound() {
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(ticketTypeRepository.findByIdWithLock(ticketTypeId)).thenReturn(Optional.empty());

      assertThrows(TicketTypeNotFoundException.class,
          () -> ticketTypeService.purchaseTicket(userId, ticketTypeId));
    }

    @Test
    @DisplayName("should throw SalesPeriodException when sales not started")
    void shouldThrowSalesPeriodExceptionWhenSalesNotStarted() {
      event.setSalesStart(LocalDateTime.now().plusDays(1));

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(ticketTypeRepository.findByIdWithLock(ticketTypeId)).thenReturn(Optional.of(ticketType));

      assertThrows(SalesPeriodException.class,
          () -> ticketTypeService.purchaseTicket(userId, ticketTypeId));
    }

    @Test
    @DisplayName("should throw SalesPeriodException when sales ended")
    void shouldThrowSalesPeriodExceptionWhenSalesEnded() {
      event.setSalesStart(LocalDateTime.now().minusDays(10));
      event.setSalesEnd(LocalDateTime.now().minusDays(1));

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(ticketTypeRepository.findByIdWithLock(ticketTypeId)).thenReturn(Optional.of(ticketType));

      assertThrows(SalesPeriodException.class,
          () -> ticketTypeService.purchaseTicket(userId, ticketTypeId));
    }

    @Test
    @DisplayName("should throw SalesPeriodException when event has ended")
    void shouldThrowSalesPeriodExceptionWhenEventEnded() {
      event.setSalesStart(null);
      event.setSalesEnd(null);
      event.setEnd(LocalDateTime.now().minusDays(1));

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(ticketTypeRepository.findByIdWithLock(ticketTypeId)).thenReturn(Optional.of(ticketType));

      assertThrows(SalesPeriodException.class,
          () -> ticketTypeService.purchaseTicket(userId, ticketTypeId));
    }

    @Test
    @DisplayName("should throw TicketsSoldOutException when sold out")
    void shouldThrowTicketsSoldOutWhenSoldOut() {
      ticketType.setTotalAvailable(10);

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(ticketTypeRepository.findByIdWithLock(ticketTypeId)).thenReturn(Optional.of(ticketType));
      when(ticketRepository.countByTicketTypeId(ticketTypeId)).thenReturn(10);

      assertThrows(TicketsSoldOutException.class,
          () -> ticketTypeService.purchaseTicket(userId, ticketTypeId));
    }

    @Test
    @DisplayName("should allow purchase when totalAvailable is null (unlimited)")
    void shouldAllowPurchaseWhenUnlimited() {
      ticketType.setTotalAvailable(null);

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(ticketTypeRepository.findByIdWithLock(ticketTypeId)).thenReturn(Optional.of(ticketType));
      when(ticketRepository.countByTicketTypeId(ticketTypeId)).thenReturn(1000);
      when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> {
        Ticket t = i.getArgument(0);
        t.setId(UUID.randomUUID());
        return t;
      });

      Ticket result = ticketTypeService.purchaseTicket(userId, ticketTypeId);

      assertNotNull(result);
    }
  }
}
