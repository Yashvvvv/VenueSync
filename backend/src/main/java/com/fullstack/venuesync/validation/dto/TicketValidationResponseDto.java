package com.fullstack.venuesync.validation.dto;

import java.util.UUID;

import com.fullstack.venuesync.validation.domain.TicketValidationStatusEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketValidationResponseDto {
  private UUID ticketId;
  private TicketValidationStatusEnum status;
}
