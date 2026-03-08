package com.fullstack.venuesync.events.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

import com.fullstack.venuesync.events.domain.EventStatusEnum;
import com.fullstack.venuesync.tickets.dto.CreateTicketTypeRequestDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateEventRequestDto {

  @NotBlank(message = "Event name is required")
  @Size(min = 2, max = 200, message = "Event name must be between 2 and 200 characters")
  private String name;

  private LocalDateTime start;

  private LocalDateTime end;

  @NotBlank(message = "Venue information is required")
  @Size(min = 2, max = 500, message = "Venue must be between 2 and 500 characters")
  private String venue;

  private LocalDateTime salesStart;

  private LocalDateTime salesEnd;

  @NotNull(message = "Event status must be provided")
  private EventStatusEnum status;

  @NotEmpty(message = "At least one ticket type is required")
  @Valid
  private List<CreateTicketTypeRequestDto> ticketTypes;
}
