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
  private TicketStatusEnum status;
  private Double price;
  private String description;
  private String eventName;
  private String eventVenue;
  private LocalDateTime eventStart;
  private LocalDateTime eventEnd;
}
