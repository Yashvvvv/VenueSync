package com.fullstack.venuesync.validation.dto;

import com.fullstack.venuesync.validation.domain.TicketValidationMethod;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketValidationRequestDto {
  private String id;
  private TicketValidationMethod method;
}
