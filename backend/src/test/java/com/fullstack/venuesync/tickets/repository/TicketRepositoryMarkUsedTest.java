package com.fullstack.venuesync.tickets.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fullstack.venuesync.tickets.domain.Ticket;
import com.fullstack.venuesync.tickets.domain.TicketStatusEnum;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/**
 * The "admitted once" guarantee lives in SQL, so it's tested against a real database, not a mock:
 * the conditional UPDATE matches a PURCHASED ticket exactly once.
 */
@DataJpaTest
class TicketRepositoryMarkUsedTest {

  @Autowired
  private TicketRepository ticketRepository;

  @Autowired
  private TestEntityManager entityManager;

  private UUID persistTicket(TicketStatusEnum status) {
    Ticket ticket = new Ticket();
    ticket.setStatus(status);
    return entityManager.persistAndFlush(ticket).getId();
  }

  private int markUsed(UUID id) {
    return ticketRepository.markUsed(id, TicketStatusEnum.PURCHASED, TicketStatusEnum.USED, LocalDateTime.now());
  }

  @Test
  @DisplayName("admits a purchased ticket once; a second admit matches nothing")
  void admitsOnce() {
    UUID id = persistTicket(TicketStatusEnum.PURCHASED);

    assertEquals(1, markUsed(id));
    assertEquals(0, markUsed(id));

    entityManager.clear();
    assertEquals(TicketStatusEnum.USED, ticketRepository.findById(id).orElseThrow().getStatus());
  }

  @Test
  @DisplayName("never admits a ticket that isn't PURCHASED")
  void neverAdmitsOtherStatuses() {
    assertEquals(0, markUsed(persistTicket(TicketStatusEnum.CANCELLED)));
    assertEquals(0, markUsed(persistTicket(TicketStatusEnum.EXPIRED)));
    assertEquals(0, markUsed(persistTicket(TicketStatusEnum.USED)));
  }
}
