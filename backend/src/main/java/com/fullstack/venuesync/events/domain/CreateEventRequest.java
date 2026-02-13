package com.fullstack.venuesync.events.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fullstack.venuesync.tickets.domain.CreateTicketTypeRequest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateEventRequest {

  private String name;
  private LocalDateTime start;
  private LocalDateTime end;
  private String venue;
  private LocalDateTime salesStart;
  private LocalDateTime salesEnd;
  private EventStatusEnum status;
  private List<CreateTicketTypeRequest> ticketTypes = new ArrayList<>();
}
