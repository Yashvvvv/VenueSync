package com.fullstack.venuesync.tickets.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.fullstack.venuesync.tickets.dto.GetTicketResponseDto;
import com.fullstack.venuesync.tickets.dto.ListTicketResponseDto;
import com.fullstack.venuesync.tickets.dto.ListTicketTicketTypeResponseDto;
import com.fullstack.venuesync.tickets.domain.Ticket;
import com.fullstack.venuesync.tickets.domain.TicketCodes;
import com.fullstack.venuesync.tickets.domain.TicketType;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TicketMapper {

  ListTicketTicketTypeResponseDto toListTicketTicketTypeResponseDto(TicketType ticketType);

  @Mapping(target = "eventName", source = "ticketType.event.name")
  @Mapping(target = "eventStart", source = "ticketType.event.start")
  @Mapping(target = "eventEnd", source = "ticketType.event.end")
  @Mapping(target = "ticketCode", expression = "java(ticketCode(ticket))")
  ListTicketResponseDto toListTicketResponseDto(Ticket ticket);

  @Mapping(target = "ticketCode", expression = "java(ticketCode(ticket))")
  @Mapping(target = "ticketTypeName", source = "ticketType.name")
  @Mapping(target = "eventId", source = "ticketType.event.id")
  @Mapping(target = "purchasedAt", source = "createdAt")
  @Mapping(target = "price", source = "ticketType.price")
  @Mapping(target = "description", source = "ticketType.description")
  @Mapping(target = "eventName", source = "ticketType.event.name")
  @Mapping(target = "eventVenue", source = "ticketType.event.venue")
  @Mapping(target = "eventStart", source = "ticketType.event.start")
  @Mapping(target = "eventEnd", source = "ticketType.event.end")
  GetTicketResponseDto toGetTicketResponseDto(Ticket ticket);

  default String ticketCode(Ticket ticket) {
    return ticket.getId() == null ? null : TicketCodes.of(ticket.getId());
  }

}
