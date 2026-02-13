package com.fullstack.venuesync.events.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fullstack.venuesync.events.domain.EventStatusEnum;
import com.fullstack.venuesync.tickets.dto.ListEventTicketTypeResponseDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListEventResponseDto {

  private UUID id;
  private String name;
  private LocalDateTime start;
  private LocalDateTime end;
  private String venue;
  private LocalDateTime salesStart;
  private LocalDateTime salesEnd;
  private EventStatusEnum status;
  private List<ListEventTicketTypeResponseDto> ticketTypes = new ArrayList<>();
}
