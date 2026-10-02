package com.fullstack.venuesync.validation.dto;

import java.util.UUID;

import com.fullstack.venuesync.validation.domain.TicketValidationMethod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketValidationRequestDto {

  @NotBlank(message = "Ticket or QR code ID is required")
  private String id;

  @NotNull(message = "Validation method is required")
  private TicketValidationMethod method;

  /** Optional: the event this scanner admits for. A ticket for another event is WRONG_EVENT and left untouched. */
  private UUID eventId;
}
