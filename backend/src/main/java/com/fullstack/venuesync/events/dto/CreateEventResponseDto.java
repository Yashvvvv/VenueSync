package com.fullstack.venuesync.events.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fullstack.venuesync.events.domain.EventStatusEnum;
import com.fullstack.venuesync.tickets.dto.CreateTicketTypeResponseDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateEventResponseDto {

  private UUID id;
  private String name;
  private LocalDateTime start;
  private LocalDateTime end;
  private String venue;
  /** The event's photo, relative to the API origin; null without one (clients show a stand-in). */
  private String imageUrl;
  private LocalDateTime salesStart;
  private LocalDateTime salesEnd;
  private EventStatusEnum status;
  /** Send it back on update: an update from an older copy is refused. */
  private Long version;
  private List<CreateTicketTypeResponseDto> ticketTypes;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
