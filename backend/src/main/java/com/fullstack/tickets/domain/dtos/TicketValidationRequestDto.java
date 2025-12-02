package com.fullstack.tickets.domain.dtos;

import com.fullstack.tickets.domain.entities.TicketValidationMethod;
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
