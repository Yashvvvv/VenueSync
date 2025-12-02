package com.fullstack.tickets.mappers;

import com.fullstack.tickets.domain.dtos.GetTicketResponseDto;
import com.fullstack.tickets.domain.dtos.ListTicketResponseDto;
import com.fullstack.tickets.domain.dtos.ListTicketTicketTypeResponseDto;
import com.fullstack.tickets.domain.entities.Ticket;
import com.fullstack.tickets.domain.entities.TicketType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TicketMapper {

  ListTicketTicketTypeResponseDto toListTicketTicketTypeResponseDto(TicketType ticketType);

  @Mapping(target = "eventName", source = "ticketType.event.name")
  @Mapping(target = "eventStart", source = "ticketType.event.start")
  @Mapping(target = "eventEnd", source = "ticketType.event.end")
  ListTicketResponseDto toListTicketResponseDto(Ticket ticket);

  @Mapping(target = "price", source = "ticketType.price")
  @Mapping(target = "description", source = "ticketType.description")
  @Mapping(target = "eventName", source = "ticketType.event.name")
  @Mapping(target = "eventVenue", source = "ticketType.event.venue")
  @Mapping(target = "eventStart", source = "ticketType.event.start")
  @Mapping(target = "eventEnd", source = "ticketType.event.end")
  GetTicketResponseDto toGetTicketResponseDto(Ticket ticket);

}
