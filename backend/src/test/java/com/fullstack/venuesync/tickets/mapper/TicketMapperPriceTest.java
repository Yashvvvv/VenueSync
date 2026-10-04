package com.fullstack.venuesync.tickets.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fullstack.venuesync.tickets.domain.Ticket;
import com.fullstack.venuesync.tickets.domain.TicketType;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/** A ticket shows what its buyer paid, not what its type costs today. */
class TicketMapperPriceTest {

  private final TicketMapper mapper = Mappers.getMapper(TicketMapper.class);

  private Ticket ticket(Double paid, double typePriceNow) {
    TicketType type = new TicketType();
    type.setPrice(typePriceNow);
    Ticket ticket = new Ticket();
    ticket.setTicketType(type);
    ticket.setPricePaid(paid);
    return ticket;
  }

  @Test
  void aRepricedTypeDoesNotRewriteWhatWasPaid() {
    Ticket bought = ticket(25.0, 40.0);

    assertEquals(25.0, mapper.toGetTicketResponseDto(bought).getPrice());
    assertEquals(25.0, mapper.toListTicketResponseDto(bought).getPrice());
  }

  @Test
  void aTicketFromBeforePricePaidShowsItsTypePrice() {
    Ticket old = ticket(null, 40.0);

    assertEquals(40.0, mapper.toGetTicketResponseDto(old).getPrice());
    assertEquals(40.0, mapper.toListTicketResponseDto(old).getPrice());
  }
}
