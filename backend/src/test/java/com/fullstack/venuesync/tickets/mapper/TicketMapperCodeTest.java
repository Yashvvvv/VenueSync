package com.fullstack.venuesync.tickets.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fullstack.venuesync.tickets.domain.Ticket;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class TicketMapperCodeTest {

  private final TicketMapper mapper = Mappers.getMapper(TicketMapper.class);

  @Test
  void ticketResponsesCarryTheShortCode() {
    Ticket ticket = new Ticket();
    ticket.setId(UUID.fromString("f5a3038b-3412-46f9-8f10-33fc236d6b17"));

    assertEquals("F5A3-038B", mapper.toGetTicketResponseDto(ticket).getTicketCode());
    assertEquals("F5A3-038B", mapper.toListTicketResponseDto(ticket).getTicketCode());
  }
}
