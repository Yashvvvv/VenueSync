package com.fullstack.venuesync.validation.service;

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
import com.fullstack.venuesync.tickets.domain.Ticket;
import com.fullstack.venuesync.tickets.domain.TicketStatusEnum;
import com.fullstack.venuesync.tickets.domain.TicketType;
import com.fullstack.venuesync.tickets.repository.TicketRepository;
import com.fullstack.venuesync.validation.domain.QrCode;
import com.fullstack.venuesync.validation.domain.QrCodeStatusEnum;
import com.fullstack.venuesync.validation.domain.TicketValidation;
import com.fullstack.venuesync.validation.domain.TicketValidationMethod;
import com.fullstack.venuesync.validation.domain.TicketValidationStatusEnum;
import com.fullstack.venuesync.validation.repository.QrCodeRepository;
import com.fullstack.venuesync.validation.repository.TicketValidationRepository;

@ExtendWith(MockitoExtension.class)
class TicketValidationServiceImplTest {

  @Mock
  private QrCodeRepository qrCodeRepository;

  @Mock
  private TicketValidationRepository ticketValidationRepository;

  @Mock
  private TicketRepository ticketRepository;

  @InjectMocks
  private TicketValidationServiceImpl ticketValidationService;

  private UUID qrCodeId;
  private UUID ticketId;
  private Ticket ticket;
  private QrCode qrCode;
  private Event event;
  private TicketType ticketType;

  @BeforeEach
  void setUp() {
    qrCodeId = UUID.randomUUID();
    ticketId = UUID.randomUUID();

    event = new Event();
    event.setId(UUID.randomUUID());
    event.setEnd(LocalDateTime.now().plusDays(5));
    event.setStatus(EventStatusEnum.PUBLISHED);

    ticketType = new TicketType();
    ticketType.setId(UUID.randomUUID());
    ticketType.setEvent(event);

    ticket = new Ticket();
    ticket.setId(ticketId);
    ticket.setStatus(TicketStatusEnum.PURCHASED);
    ticket.setTicketType(ticketType);
    ticket.setValidations(new ArrayList<>());

    qrCode = new QrCode();
    qrCode.setId(qrCodeId);
    qrCode.setTicket(ticket);
    qrCode.setStatus(QrCodeStatusEnum.ACTIVE);
  }

  @Nested
  @DisplayName("validateTicketByQrCode")
  class ValidateByQrCodeTests {

    @Test
    @DisplayName("should return VALID for valid QR code with purchased ticket")
    void shouldReturnValidForPurchasedTicket() {
      when(qrCodeRepository.findByIdAndStatus(qrCodeId, QrCodeStatusEnum.ACTIVE))
          .thenReturn(Optional.of(qrCode));
      when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));
      when(ticketValidationRepository.save(any(TicketValidation.class)))
          .thenAnswer(i -> i.getArgument(0));

      TicketValidation result = ticketValidationService.validateTicketByQrCode(qrCodeId);

      assertEquals(TicketValidationStatusEnum.VALID, result.getStatus());
      assertEquals(TicketValidationMethod.QR_SCAN, result.getValidationMethod());
      assertEquals(ticket, result.getTicket());
    }

    @Test
    @DisplayName("should return INVALID when QR code not found")
    void shouldReturnInvalidWhenQrCodeNotFound() {
      when(qrCodeRepository.findByIdAndStatus(qrCodeId, QrCodeStatusEnum.ACTIVE))
          .thenReturn(Optional.empty());
      when(ticketValidationRepository.save(any(TicketValidation.class)))
          .thenAnswer(i -> i.getArgument(0));

      TicketValidation result = ticketValidationService.validateTicketByQrCode(qrCodeId);

      assertEquals(TicketValidationStatusEnum.INVALID, result.getStatus());
      assertEquals(TicketValidationMethod.QR_SCAN, result.getValidationMethod());
    }

    @Test
    @DisplayName("should return EXPIRED when ticket is expired")
    void shouldReturnExpiredWhenTicketExpired() {
      ticket.setStatus(TicketStatusEnum.EXPIRED);

      when(qrCodeRepository.findByIdAndStatus(qrCodeId, QrCodeStatusEnum.ACTIVE))
          .thenReturn(Optional.of(qrCode));
      when(ticketValidationRepository.save(any(TicketValidation.class)))
          .thenAnswer(i -> i.getArgument(0));

      TicketValidation result = ticketValidationService.validateTicketByQrCode(qrCodeId);

      assertEquals(TicketValidationStatusEnum.EXPIRED, result.getStatus());
    }

    @Test
    @DisplayName("should return EXPIRED when event has ended")
    void shouldReturnExpiredWhenEventEnded() {
      event.setEnd(LocalDateTime.now().minusDays(1));

      when(qrCodeRepository.findByIdAndStatus(qrCodeId, QrCodeStatusEnum.ACTIVE))
          .thenReturn(Optional.of(qrCode));
      when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));
      when(ticketValidationRepository.save(any(TicketValidation.class)))
          .thenAnswer(i -> i.getArgument(0));

      TicketValidation result = ticketValidationService.validateTicketByQrCode(qrCodeId);

      assertEquals(TicketValidationStatusEnum.EXPIRED, result.getStatus());
    }

    @Test
    @DisplayName("should return ALREADY_USED when ticket was already used")
    void shouldReturnAlreadyUsedWhenTicketUsed() {
      ticket.setStatus(TicketStatusEnum.USED);

      when(qrCodeRepository.findByIdAndStatus(qrCodeId, QrCodeStatusEnum.ACTIVE))
          .thenReturn(Optional.of(qrCode));
      when(ticketValidationRepository.save(any(TicketValidation.class)))
          .thenAnswer(i -> i.getArgument(0));

      TicketValidation result = ticketValidationService.validateTicketByQrCode(qrCodeId);

      assertEquals(TicketValidationStatusEnum.ALREADY_USED, result.getStatus());
    }
  }

  @Nested
  @DisplayName("validateTicketManually")
  class ValidateManuallyTests {

    @Test
    @DisplayName("should return VALID for manual validation of purchased ticket")
    void shouldReturnValidForManualValidation() {
      when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
      when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));
      when(ticketValidationRepository.save(any(TicketValidation.class)))
          .thenAnswer(i -> i.getArgument(0));

      TicketValidation result = ticketValidationService.validateTicketManually(ticketId);

      assertEquals(TicketValidationStatusEnum.VALID, result.getStatus());
      assertEquals(TicketValidationMethod.MANUAL, result.getValidationMethod());
    }

    @Test
    @DisplayName("should return INVALID when ticket not found manually")
    void shouldReturnInvalidWhenTicketNotFound() {
      when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());
      when(ticketValidationRepository.save(any(TicketValidation.class)))
          .thenAnswer(i -> i.getArgument(0));

      TicketValidation result = ticketValidationService.validateTicketManually(ticketId);

      assertEquals(TicketValidationStatusEnum.INVALID, result.getStatus());
      assertEquals(TicketValidationMethod.MANUAL, result.getValidationMethod());
    }
  }
}
