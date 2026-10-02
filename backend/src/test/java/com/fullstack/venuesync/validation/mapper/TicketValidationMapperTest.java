package com.fullstack.venuesync.validation.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.tickets.domain.Ticket;
import com.fullstack.venuesync.tickets.domain.TicketType;
import com.fullstack.venuesync.validation.domain.TicketValidation;
import com.fullstack.venuesync.validation.domain.TicketValidationStatusEnum;
import com.fullstack.venuesync.validation.dto.TicketValidationResponseDto;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class TicketValidationMapperTest {

  private final TicketValidationMapper mapper = Mappers.getMapper(TicketValidationMapper.class);

  @Test
  void responseSaysWhatWasScanned() {
    Event event = new Event();
    event.setName("Summer Vibes");
    TicketType type = new TicketType();
    type.setName("VIP Experience");
    type.setEvent(event);
    Ticket ticket = new Ticket();
    ticket.setId(UUID.randomUUID());
    ticket.setTicketType(type);
    TicketValidation validation = new TicketValidation();
    validation.setTicket(ticket);
    validation.setStatus(TicketValidationStatusEnum.VALID);

    TicketValidationResponseDto dto = mapper.toTicketValidationResponseDto(validation);

    assertEquals(ticket.getId(), dto.getTicketId());
    assertEquals("Summer Vibes", dto.getEventName());
    assertEquals("VIP Experience", dto.getTicketTypeName());
  }

  @Test
  void invalidScanHasNoTicketDetails() {
    TicketValidation validation = new TicketValidation();
    validation.setStatus(TicketValidationStatusEnum.INVALID);

    TicketValidationResponseDto dto = mapper.toTicketValidationResponseDto(validation);

    assertEquals(TicketValidationStatusEnum.INVALID, dto.getStatus());
    assertNull(dto.getEventName());
    assertNull(dto.getTicketTypeName());
  }
}
