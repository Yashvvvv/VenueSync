package com.fullstack.venuesync.events.repository;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.events.domain.EventImage;
import com.fullstack.venuesync.events.domain.EventStatusEnum;
import java.time.LocalDateTime;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/** Photo bytes and the photo's timestamp against a real database: the column types are the point. */
@DataJpaTest
class EventImageRepositoryTest {

  @Autowired private EventImageRepository imageRepository;
  @Autowired private EventRepository eventRepository;
  @Autowired private TestEntityManager entityManager;

  @Test
  void aPhotoOfRealSizeRoundTrips() {
    Event event = new Event();
    event.setName("Show");
    event.setVenue("Hall");
    event.setStatus(EventStatusEnum.PUBLISHED);
    event = entityManager.persistAndFlush(event);

    byte[] data = new byte[1_500_000];
    new Random(7).nextBytes(data);
    imageRepository.saveAndFlush(new EventImage(event.getId(), "image/jpeg", data));
    entityManager.clear();

    EventImage read = imageRepository.findById(event.getId()).orElseThrow();
    assertEquals("image/jpeg", read.getContentType());
    assertArrayEquals(data, read.getData());
  }

  @Test
  void settingAPhotoDoesNotBumpTheEventsVersion() {
    Event event = new Event();
    event.setName("Show");
    event.setVenue("Hall");
    event.setStatus(EventStatusEnum.DRAFT);
    event = entityManager.persistAndFlush(event);
    long before = event.getVersion();
    LocalDateTime at = LocalDateTime.of(2026, 10, 5, 12, 0, 0, 123_000_000);

    eventRepository.setImageUpdatedAt(event.getId(), at);
    Event read = eventRepository.findById(event.getId()).orElseThrow();
    assertEquals(before, read.getVersion()); // an organizer's open form must still save afterwards
    assertEquals(Event.imageUrl(read.getId(), at), read.getImageUrl()); // the URL survives the round trip
  }
}
