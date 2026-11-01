package com.fullstack.venuesync.tickets.stress;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.events.domain.EventStatusEnum;
import com.fullstack.venuesync.events.repository.EventRepository;
import com.fullstack.venuesync.shared.domain.User;
import com.fullstack.venuesync.shared.domain.UserRepository;
import com.fullstack.venuesync.tickets.domain.TicketType;
import com.fullstack.venuesync.tickets.exception.TicketsSoldOutException;
import com.fullstack.venuesync.tickets.repository.TicketRepository;
import com.fullstack.venuesync.tickets.repository.TicketTypeRepository;
import com.fullstack.venuesync.tickets.service.TicketTypeService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * The pessimistic lock and idempotency key under real contention. H2's row locks don't behave like Postgres's, so this
 * runs only against a Postgres given in STRESS_DB_URL (e.g. jdbc:postgresql://localhost:55432/postgres).
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "STRESS_DB_URL", matches = ".+")
@TestPropertySource(properties = {
    "spring.datasource.url=${STRESS_DB_URL}",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.datasource.username=postgres",
    "spring.datasource.password=${STRESS_DB_PASSWORD:stress}",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"})
class ConcurrentPurchaseStressTest {

  private static final int BUYERS = 200;
  private static final int CAPACITY = 50;

  @Autowired private TicketTypeService ticketTypeService;
  @Autowired private EventRepository eventRepository;
  @Autowired private TicketTypeRepository ticketTypeRepository;
  @Autowired private TicketRepository ticketRepository;
  @Autowired private UserRepository userRepository;

  private User user() {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setName("Buyer");
    user.setEmail(user.getId() + "@stress.test");
    return userRepository.save(user);
  }

  private TicketType ticketType(int capacity) {
    Event event = new Event();
    event.setName("Stress show");
    event.setVenue("Hall");
    event.setStatus(EventStatusEnum.PUBLISHED);
    eventRepository.save(event);
    TicketType type = new TicketType();
    type.setName("GA");
    type.setPrice(499.0);
    type.setTotalAvailable(capacity);
    type.setEvent(event);
    return ticketTypeRepository.save(type);
  }

  /** Runs every task at once and returns how many purchases succeeded and how many were refused as sold out. */
  private long[] race(List<Callable<Void>> tasks) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
    long ok = 0, soldOut = 0;
    for (Future<Void> f : pool.invokeAll(tasks)) {
      try {
        f.get();
        ok++;
      } catch (java.util.concurrent.ExecutionException e) {
        if (!(e.getCause() instanceof TicketsSoldOutException)) throw e;
        soldOut++;
      }
    }
    pool.shutdown();
    return new long[] {ok, soldOut};
  }

  @RepeatedTest(5)
  void twoHundredBuyersForFiftySeatsSellExactlyFifty() throws Exception {
    TicketType type = ticketType(CAPACITY);
    List<Callable<Void>> buys = new ArrayList<>();
    for (int i = 0; i < BUYERS; i++) {
      UUID buyer = user().getId();
      buys.add(() -> {
        ticketTypeService.purchaseTicket(buyer, type.getEvent().getId(), type.getId(), UUID.randomUUID());
        return null;
      });
    }
    long start = System.nanoTime();
    long[] r = race(buys);
    System.out.printf("STRESS purchase: %d buyers, %d sold, %d sold out, %d ms%n",
        BUYERS, r[0], r[1], (System.nanoTime() - start) / 1_000_000);

    assertEquals(CAPACITY, r[0]);
    assertEquals(BUYERS - CAPACITY, r[1]);
    assertEquals(CAPACITY, ticketRepository.countByTicketTypeId(type.getId()));
  }

  @RepeatedTest(5)
  void fiftyConcurrentRetriesOfOnePurchaseMakeOneTicket() throws Exception {
    TicketType type = ticketType(CAPACITY);
    UUID buyer = user().getId();
    UUID key = UUID.randomUUID();
    List<Callable<Void>> retries = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
      retries.add(() -> {
        ticketTypeService.purchaseTicket(buyer, type.getEvent().getId(), type.getId(), key);
        return null;
      });
    }
    long[] r = race(retries);
    long tickets = ticketRepository.countByTicketTypeId(type.getId());
    System.out.printf("STRESS idempotency: 50 retries, %d returned a ticket, %d tickets exist%n", r[0], tickets);

    assertEquals(50, r[0]);
    assertEquals(1, tickets);
  }
}
