package com.fullstack.venuesync.shared.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.events.domain.EventStatusEnum;
import com.fullstack.venuesync.events.repository.EventImageRepository;
import com.fullstack.venuesync.events.repository.EventRepository;
import com.fullstack.venuesync.events.service.EventImageService;
import com.fullstack.venuesync.tickets.domain.TicketType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

/** The demo catalogue against a real database: a US demo event becomes its Indian one in place, once. */
@DataJpaTest
@Import({DataLoader.class, EventImageService.class})
class DataLoaderTest {

  @Autowired private DataLoader loader;
  @Autowired private EventRepository eventRepository;
  @Autowired private EventImageRepository imageRepository;
  @Autowired private TestEntityManager entityManager;

  private UUID persistUsDemo() {
    Event event = new Event();
    event.setName("Summer Vibes Music Festival");
    event.setVenue("Central Park Amphitheater, New York, NY");
    event.setStatus(EventStatusEnum.PUBLISHED);
    event.setStart(LocalDateTime.now().plusDays(10));
    event.setEnd(LocalDateTime.now().plusDays(10).plusHours(8));
    TicketType premium = new TicketType();
    premium.setName("Premium");
    premium.setPrice(149.99);
    premium.setTotalAvailable(700); // more than the Indian tier's 500: never lowered
    premium.setEvent(event);
    event.setTicketTypes(new ArrayList<>(List.of(premium)));
    return entityManager.persistAndFlush(event).getId();
  }

  @Test
  void aUsDemoEventIsRewrittenInPlaceWithItsPhotoAndOnlyOnce() {
    // Spring already ran the loader once at startup (an empty database: all twelve created). Put one back to its US
    // original, as on a database seeded before the rewrite.
    eventRepository.findFirstByNameAndVenue("Monsoon Beats Music Festival", "Mahalaxmi Racecourse, Mumbai").ifPresent(e -> {
      imageRepository.deleteById(e.getId());
      eventRepository.delete(e);
    });
    entityManager.flush();
    UUID id = persistUsDemo();

    loader.run();
    entityManager.clear();

    Event event = eventRepository.findById(id).orElseThrow();
    assertEquals("Monsoon Beats Music Festival", event.getName());
    assertEquals("Mahalaxmi Racecourse, Mumbai", event.getVenue());
    TicketType gold = event.getTicketTypes().stream().filter(t -> t.getName().equals("Gold")).findFirst().orElseThrow();
    assertEquals(2999.0, gold.getPrice());
    assertEquals(700, gold.getTotalAvailable());
    assertNotNull(event.getImageUrl());
    assertTrue(imageRepository.existsById(id));

    long events = eventRepository.count();
    loader.run();
    assertEquals(events, eventRepository.count()); // the second start changes nothing
    assertEquals(12, events);
  }
}
