package com.fullstack.venuesync.validation.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.fullstack.venuesync.validation.dto.TicketValidationResponseDto;
import com.fullstack.venuesync.validation.domain.TicketValidation;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TicketValidationMapper {

  @Mapping(target = "ticketId", source = "ticket.id")
  TicketValidationResponseDto toTicketValidationResponseDto(TicketValidation ticketValidation);

}
