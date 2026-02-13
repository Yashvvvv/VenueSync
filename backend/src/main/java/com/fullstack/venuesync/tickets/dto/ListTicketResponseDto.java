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
  private TicketStatusEnum status;
  private ListTicketTicketTypeResponseDto ticketType;
  private String eventName;
  private LocalDateTime eventStart;
  private LocalDateTime eventEnd;
}
