package com.fullstack.venuesync.validation.service;

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
import com.fullstack.venuesync.staff.exception.NotEventStaffException;
import com.fullstack.venuesync.staff.service.EventStaffService;
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

  @Mock
  private EventStaffService eventStaffService; // a void mock: allows unless a test makes it throw

  @InjectMocks
  private TicketValidationServiceImpl ticketValidationService;

  private final UUID userId = UUID.randomUUID();
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
      when(ticketRepository.markUsed(eq(ticketId), eq(TicketStatusEnum.PURCHASED), eq(TicketStatusEnum.USED), any()))
          .thenReturn(1);
      when(ticketValidationRepository.save(any(TicketValidation.class)))
          .thenAnswer(i -> i.getArgument(0));

      TicketValidation result = ticketValidationService.validateTicketByQrCode(qrCodeId, userId, null, null);

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

      TicketValidation result = ticketValidationService.validateTicketByQrCode(qrCodeId, userId, null, null);

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

      TicketValidation result = ticketValidationService.validateTicketByQrCode(qrCodeId, userId, null, null);

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

      TicketValidation result = ticketValidationService.validateTicketByQrCode(qrCodeId, userId, null, null);

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

      TicketValidation result = ticketValidationService.validateTicketByQrCode(qrCodeId, userId, null, null);

      assertEquals(TicketValidationStatusEnum.ALREADY_USED, result.getStatus());
    }

    @Test
    @DisplayName("should return ALREADY_USED when a simultaneous scan admitted the ticket first")
    void shouldReturnAlreadyUsedWhenRaceLost() {
      // Both scans read PURCHASED; the other one's UPDATE matched first, so ours matches 0 rows.
      when(qrCodeRepository.findByIdAndStatus(qrCodeId, QrCodeStatusEnum.ACTIVE))
          .thenReturn(Optional.of(qrCode));
      when(ticketRepository.markUsed(eq(ticketId), eq(TicketStatusEnum.PURCHASED), eq(TicketStatusEnum.USED), any()))
          .thenReturn(0);
      when(ticketValidationRepository.save(any(TicketValidation.class)))
          .thenAnswer(i -> i.getArgument(0));

      TicketValidation result = ticketValidationService.validateTicketByQrCode(qrCodeId, userId, null, null);

      assertEquals(TicketValidationStatusEnum.ALREADY_USED, result.getStatus());
    }

    @Test
    @DisplayName("should return INVALID for a cancelled ticket and never admit it")
    void shouldReturnInvalidWhenTicketCancelled() {
      ticket.setStatus(TicketStatusEnum.CANCELLED);
      when(qrCodeRepository.findByIdAndStatus(qrCodeId, QrCodeStatusEnum.ACTIVE))
          .thenReturn(Optional.of(qrCode));
      when(ticketValidationRepository.save(any(TicketValidation.class)))
          .thenAnswer(i -> i.getArgument(0));

      TicketValidation result = ticketValidationService.validateTicketByQrCode(qrCodeId, userId, null, null);

      assertEquals(TicketValidationStatusEnum.INVALID, result.getStatus());
      verify(ticketRepository, never()).markUsed(any(), any(), any(), any());
    }
  }

  @Test
  @DisplayName("should return INVALID for a ticket to a cancelled event and never admit it")
  void shouldReturnInvalidWhenEventCancelled() {
    event.setStatus(EventStatusEnum.CANCELLED); // the ticket itself still says PURCHASED (bought in the same instant)
    when(qrCodeRepository.findByIdAndStatus(qrCodeId, QrCodeStatusEnum.ACTIVE))
        .thenReturn(Optional.of(qrCode));
    when(ticketValidationRepository.save(any(TicketValidation.class)))
        .thenAnswer(i -> i.getArgument(0));

    TicketValidation result = ticketValidationService.validateTicketByQrCode(qrCodeId, userId, null, null);

    assertEquals(TicketValidationStatusEnum.INVALID, result.getStatus());
    verify(ticketRepository, never()).markUsed(any(), any(), any(), any());
  }

  @Nested
  @DisplayName("Idempotency-Key")
  class IdempotencyTests {

    @Test
    @DisplayName("a retry with a known key returns the first answer and validates nothing again")
    void replaysFirstAnswer() {
      UUID key = UUID.randomUUID();
      TicketValidation first = new TicketValidation();
      first.setStatus(TicketValidationStatusEnum.VALID);
      when(ticketValidationRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(first));

      TicketValidation result = ticketValidationService.validateTicketByQrCode(qrCodeId, userId, key, null);

      assertSame(first, result); // VALID again, not ALREADY_USED about the ticket this scan admitted
      verifyNoInteractions(qrCodeRepository, ticketRepository);
      verify(ticketValidationRepository, never()).save(any());
    }

    @Test
    @DisplayName("a new key is stored with the validation")
    void storesKey() {
      UUID key = UUID.randomUUID();
      when(ticketValidationRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());
      when(qrCodeRepository.findByIdAndStatus(qrCodeId, QrCodeStatusEnum.ACTIVE)).thenReturn(Optional.of(qrCode));
      when(ticketRepository.markUsed(eq(ticketId), eq(TicketStatusEnum.PURCHASED), eq(TicketStatusEnum.USED), any()))
          .thenReturn(1);
      when(ticketValidationRepository.save(any(TicketValidation.class))).thenAnswer(i -> i.getArgument(0));

      TicketValidation result = ticketValidationService.validateTicketByQrCode(qrCodeId, userId, key, null);

      assertEquals(TicketValidationStatusEnum.VALID, result.getStatus());
      assertEquals(key, result.getIdempotencyKey());
    }
  }

  @Nested
  @DisplayName("event scoping")
  class EventScopeTests {

    @Test
    @DisplayName("a ticket for another event is WRONG_EVENT, never admitted and never stored")
    void wrongEventLeavesTicketUntouched() {
      when(qrCodeRepository.findByIdAndStatus(qrCodeId, QrCodeStatusEnum.ACTIVE)).thenReturn(Optional.of(qrCode));

      TicketValidation result = ticketValidationService.validateTicketByQrCode(qrCodeId, userId, null, UUID.randomUUID());

      assertEquals(TicketValidationStatusEnum.WRONG_EVENT, result.getStatus());
      assertEquals(ticket, result.getTicket()); // the response can still say which event it's for
      verify(ticketRepository, never()).markUsed(any(), any(), any(), any());
      verify(ticketValidationRepository, never()).save(any()); // DB CHECK constraint doesn't allow WRONG_EVENT
    }

    @Test
    @DisplayName("a ticket for the scanner's event is admitted as usual")
    void rightEventAdmits() {
      when(qrCodeRepository.findByIdAndStatus(qrCodeId, QrCodeStatusEnum.ACTIVE)).thenReturn(Optional.of(qrCode));
      when(ticketRepository.markUsed(eq(ticketId), eq(TicketStatusEnum.PURCHASED), eq(TicketStatusEnum.USED), any()))
          .thenReturn(1);
      when(ticketValidationRepository.save(any(TicketValidation.class))).thenAnswer(i -> i.getArgument(0));

      TicketValidation result = ticketValidationService.validateTicketByQrCode(qrCodeId, userId, null, event.getId());

      assertEquals(TicketValidationStatusEnum.VALID, result.getStatus());
    }
  }

  @Nested
  @DisplayName("ticket code")
  class TicketCodeTests {

    @Test
    @DisplayName("exactly one ticket with the code in the scanner's event is validated")
    void oneMatchIsValidated() {
      when(ticketRepository.findByEventAndCodePrefix(eq(event.getId()), eq("f5a3038b"), any())).thenReturn(List.of(ticket));
      when(ticketRepository.markUsed(eq(ticketId), eq(TicketStatusEnum.PURCHASED), eq(TicketStatusEnum.USED), any()))
          .thenReturn(1);
      when(ticketValidationRepository.save(any(TicketValidation.class))).thenAnswer(i -> i.getArgument(0));

      TicketValidation result = ticketValidationService.validateTicketByCode("f5a3038b", userId, null, event.getId());

      assertEquals(TicketValidationStatusEnum.VALID, result.getStatus());
      assertEquals(TicketValidationMethod.MANUAL, result.getValidationMethod());
    }

    @Test
    @DisplayName("no match or a clash is INVALID: the door never guesses")
    void zeroOrManyMatchesAreInvalid() {
      when(ticketValidationRepository.save(any(TicketValidation.class))).thenAnswer(i -> i.getArgument(0));
      when(ticketRepository.findByEventAndCodePrefix(any(), any(), any()))
          .thenReturn(List.of())
          .thenReturn(List.of(ticket, new Ticket()));

      assertEquals(TicketValidationStatusEnum.INVALID,
          ticketValidationService.validateTicketByCode("f5a3038b", userId, null, event.getId()).getStatus());
      assertEquals(TicketValidationStatusEnum.INVALID,
          ticketValidationService.validateTicketByCode("f5a3038b", userId, null, event.getId()).getStatus());
      verify(ticketRepository, never()).markUsed(any(), any(), any(), any());
    }
  }

  @Nested
  @DisplayName("door permission")
  class PermissionTests {

    @Test
    @DisplayName("a scanner naming an event it doesn't staff is refused before anything is looked up")
    void refusesUnstaffedDeclaredEvent() {
      UUID otherEvent = UUID.randomUUID();
      doThrow(new NotEventStaffException("no")).when(eventStaffService).requireCanScan(userId, otherEvent);

      assertThrows(NotEventStaffException.class,
          () -> ticketValidationService.validateTicketByQrCode(qrCodeId, userId, null, otherEvent));

      verifyNoInteractions(qrCodeRepository, ticketRepository);
      verify(ticketValidationRepository, never()).save(any());
    }

    @Test
    @DisplayName("without an event, the ticket's own event decides, and a stranger can't admit it")
    void refusesStrangerWithoutEvent() {
      when(qrCodeRepository.findByIdAndStatus(qrCodeId, QrCodeStatusEnum.ACTIVE)).thenReturn(Optional.of(qrCode));
      doThrow(new NotEventStaffException("no")).when(eventStaffService).requireCanScan(userId, event.getId());

      assertThrows(NotEventStaffException.class,
          () -> ticketValidationService.validateTicketByQrCode(qrCodeId, userId, null, null));

      verify(ticketRepository, never()).markUsed(any(), any(), any(), any());
      verify(ticketValidationRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("validateTicketManually")
  class ValidateManuallyTests {

    @Test
    @DisplayName("should return VALID for manual validation of purchased ticket")
    void shouldReturnValidForManualValidation() {
      when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
      when(ticketRepository.markUsed(eq(ticketId), eq(TicketStatusEnum.PURCHASED), eq(TicketStatusEnum.USED), any()))
          .thenReturn(1);
      when(ticketValidationRepository.save(any(TicketValidation.class)))
          .thenAnswer(i -> i.getArgument(0));

      TicketValidation result = ticketValidationService.validateTicketManually(ticketId, userId, null, null);

      assertEquals(TicketValidationStatusEnum.VALID, result.getStatus());
      assertEquals(TicketValidationMethod.MANUAL, result.getValidationMethod());
    }

    @Test
    @DisplayName("should return INVALID when ticket not found manually")
    void shouldReturnInvalidWhenTicketNotFound() {
      when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());
      when(ticketValidationRepository.save(any(TicketValidation.class)))
          .thenAnswer(i -> i.getArgument(0));

      TicketValidation result = ticketValidationService.validateTicketManually(ticketId, userId, null, null);

      assertEquals(TicketValidationStatusEnum.INVALID, result.getStatus());
      assertEquals(TicketValidationMethod.MANUAL, result.getValidationMethod());
    }
  }
}
