package com.fullstack.venuesync.tickets.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fullstack.venuesync.tickets.domain.TicketStatusEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A data transfer object representing the response for retrieving ticket details.
 * This class contains information about the ticket, its associated event, and its status.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetTicketResponseDto {
  private UUID id;
  /** Short code for manual check-in, e.g. F5A3-038B (see TicketCodes). */
  private String ticketCode;
  private TicketStatusEnum status;
  private String ticketTypeName;
  private Double price;
  private String description;
  private UUID eventId;
  private String eventName;
  private String eventVenue;
  private LocalDateTime eventStart;
  private LocalDateTime eventEnd;
  private LocalDateTime purchasedAt;
}
