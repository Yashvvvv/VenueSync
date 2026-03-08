package com.fullstack.venuesync.validation.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Base64;
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

import com.fullstack.venuesync.tickets.domain.Ticket;
import com.fullstack.venuesync.tickets.domain.TicketStatusEnum;
import com.fullstack.venuesync.validation.domain.QrCode;
import com.fullstack.venuesync.validation.domain.QrCodeStatusEnum;
import com.fullstack.venuesync.validation.exception.QrCodeNotFoundException;
import com.fullstack.venuesync.validation.repository.QrCodeRepository;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.BarcodeFormat;

@ExtendWith(MockitoExtension.class)
class QrCodeServiceImplTest {

  @Mock
  private QRCodeWriter qrCodeWriter;

  @Mock
  private QrCodeRepository qrCodeRepository;

  @InjectMocks
  private QrCodeServiceImpl qrCodeService;

  private Ticket ticket;
  private UUID ticketId;
  private UUID userId;

  @BeforeEach
  void setUp() {
    ticketId = UUID.randomUUID();
    userId = UUID.randomUUID();

    ticket = new Ticket();
    ticket.setId(ticketId);
    ticket.setStatus(TicketStatusEnum.PURCHASED);
  }

  @Nested
  @DisplayName("generateQrCode")
  class GenerateQrCodeTests {

    @Test
    @DisplayName("should generate QR code successfully")
    void shouldGenerateQrCodeSuccessfully() throws Exception {
      BitMatrix bitMatrix = new BitMatrix(300, 300);
      when(qrCodeWriter.encode(anyString(), eq(BarcodeFormat.QR_CODE), eq(300), eq(300)))
          .thenReturn(bitMatrix);
      when(qrCodeRepository.saveAndFlush(any(QrCode.class))).thenAnswer(i -> i.getArgument(0));

      QrCode result = qrCodeService.generateQrCode(ticket);

      assertNotNull(result);
      assertEquals(QrCodeStatusEnum.ACTIVE, result.getStatus());
      assertEquals(ticket, result.getTicket());
      assertNotNull(result.getValue());
      verify(qrCodeRepository).saveAndFlush(any(QrCode.class));
    }

    @Test
    @DisplayName("should throw QrCodeGenerationException when encoding fails")
    void shouldThrowQrCodeGenerationExceptionOnFailure() throws Exception {
      when(qrCodeWriter.encode(anyString(), eq(BarcodeFormat.QR_CODE), eq(300), eq(300)))
          .thenThrow(new WriterException("Encoding failed"));

      assertThrows(
          com.fullstack.venuesync.validation.exception.QrCodeGenerationException.class,
          () -> qrCodeService.generateQrCode(ticket)
      );
    }
  }

  @Nested
  @DisplayName("getQrCodeImageForUserAndTicket")
  class GetQrCodeImageTests {

    @Test
    @DisplayName("should return QR code image bytes")
    void shouldReturnQrCodeImageBytes() {
      byte[] imageBytes = new byte[]{1, 2, 3, 4, 5};
      String base64Value = Base64.getEncoder().encodeToString(imageBytes);

      QrCode qrCode = new QrCode();
      qrCode.setId(UUID.randomUUID());
      qrCode.setValue(base64Value);
      qrCode.setStatus(QrCodeStatusEnum.ACTIVE);

      when(qrCodeRepository.findByTicketIdAndTicketPurchaserId(ticketId, userId))
          .thenReturn(Optional.of(qrCode));

      byte[] result = qrCodeService.getQrCodeImageForUserAndTicket(userId, ticketId);

      assertNotNull(result);
      assertArrayEquals(imageBytes, result);
    }

    @Test
    @DisplayName("should throw QrCodeNotFoundException when QR code not found")
    void shouldThrowQrCodeNotFoundWhenNotFound() {
      when(qrCodeRepository.findByTicketIdAndTicketPurchaserId(ticketId, userId))
          .thenReturn(Optional.empty());

      assertThrows(QrCodeNotFoundException.class,
          () -> qrCodeService.getQrCodeImageForUserAndTicket(userId, ticketId));
    }

    @Test
    @DisplayName("should throw QrCodeNotFoundException when base64 is invalid")
    void shouldThrowQrCodeNotFoundWhenBase64Invalid() {
      QrCode qrCode = new QrCode();
      qrCode.setId(UUID.randomUUID());
      qrCode.setValue("not-valid-base64!!!");
      qrCode.setStatus(QrCodeStatusEnum.ACTIVE);

      when(qrCodeRepository.findByTicketIdAndTicketPurchaserId(ticketId, userId))
          .thenReturn(Optional.of(qrCode));

      assertThrows(QrCodeNotFoundException.class,
          () -> qrCodeService.getQrCodeImageForUserAndTicket(userId, ticketId));
    }
  }
}
