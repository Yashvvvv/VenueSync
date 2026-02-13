package com.fullstack.venuesync.events.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.fullstack.venuesync.events.domain.CreateEventRequest;
import com.fullstack.venuesync.events.domain.UpdateEventRequest;
import com.fullstack.venuesync.events.dto.CreateEventRequestDto;
import com.fullstack.venuesync.events.dto.CreateEventResponseDto;
import com.fullstack.venuesync.events.dto.GetEventDetailsResponseDto;
import com.fullstack.venuesync.events.dto.GetEventDetailsTicketTypesResponseDto;
import com.fullstack.venuesync.events.dto.GetPublishedEventDetailsResponseDto;
import com.fullstack.venuesync.events.dto.GetPublishedEventDetailsTicketTypesResponseDto;
import com.fullstack.venuesync.events.dto.ListEventResponseDto;
import com.fullstack.venuesync.events.dto.ListPublishedEventResponseDto;
import com.fullstack.venuesync.events.dto.UpdateEventRequestDto;
import com.fullstack.venuesync.events.dto.UpdateEventResponseDto;
import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.tickets.domain.CreateTicketTypeRequest;
import com.fullstack.venuesync.tickets.domain.UpdateTicketTypeRequest;
import com.fullstack.venuesync.tickets.domain.TicketType;
import com.fullstack.venuesync.tickets.dto.CreateTicketTypeRequestDto;
import com.fullstack.venuesync.tickets.dto.ListEventTicketTypeResponseDto;
import com.fullstack.venuesync.tickets.dto.UpdateTicketTypeRequestDto;
import com.fullstack.venuesync.tickets.dto.UpdateTicketTypeResponseDto;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EventMapper {

  CreateTicketTypeRequest fromDto(CreateTicketTypeRequestDto dto);

  CreateEventRequest fromDto(CreateEventRequestDto dto);

  CreateEventResponseDto toDto(Event event);

  ListEventTicketTypeResponseDto toDto(TicketType ticketType);

  ListEventResponseDto toListEventResponseDto(Event event);

  GetEventDetailsTicketTypesResponseDto toGetEventDetailsTicketTypesResponseDto(
      TicketType ticketType);

  GetEventDetailsResponseDto toGetEventDetailsResponseDto(Event event);

  UpdateTicketTypeRequest fromDto(UpdateTicketTypeRequestDto dto);

  UpdateEventRequest fromDto(UpdateEventRequestDto dto);

  UpdateTicketTypeResponseDto toUpdateTicketTypeResponseDto(TicketType ticketType);

  UpdateEventResponseDto toUpdateEventResponseDto(Event event);

  ListPublishedEventResponseDto toListPublishedEventResponseDto(Event event);

  GetPublishedEventDetailsTicketTypesResponseDto toGetPublishedEventDetailsTicketTypesResponseDto(
      TicketType ticketType);

  GetPublishedEventDetailsResponseDto toGetPublishedEventDetailsResponseDto(Event event);
}
