package com.fullstack.venuesync.validation.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.fullstack.venuesync.validation.dto.TicketValidationResponseDto;
import com.fullstack.venuesync.validation.domain.TicketValidation;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TicketValidationMapper {

  @Mapping(target = "ticketId", source = "ticket.id")
  @Mapping(target = "eventName", source = "ticket.ticketType.event.name")
  @Mapping(target = "ticketTypeName", source = "ticket.ticketType.name")
  TicketValidationResponseDto toTicketValidationResponseDto(TicketValidation ticketValidation);

}
