package com.fullstack.venuesync.tickets.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fullstack.venuesync.tickets.domain.TicketStatusEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListTicketResponseDto {
  private UUID id;
  /** Short code for manual check-in, e.g. F5A3-038B (see TicketCodes). */
  private String ticketCode;
  private TicketStatusEnum status;
  private ListTicketTicketTypeResponseDto ticketType;
  /** What the buyer paid. ticketType.price is the type's current price, which can change after the sale. */
  private Double price;
  private String eventName;
  private LocalDateTime eventStart;
  private LocalDateTime eventEnd;
}
