package com.fullstack.venuesync.events.service;

import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.events.domain.EventImage;
import com.fullstack.venuesync.events.domain.EventImages;
import com.fullstack.venuesync.events.exception.EventImageInvalidException;
import com.fullstack.venuesync.events.exception.EventNotFoundException;
import com.fullstack.venuesync.events.repository.EventImageRepository;
import com.fullstack.venuesync.events.repository.EventRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** An event's photo: set, replaced and removed by its organizer, read by anyone. */
@Service
@RequiredArgsConstructor
public class EventImageService {

  private final EventRepository eventRepository;
  private final EventImageRepository imageRepository;

  /** Stores [data] as the event's photo and returns its new image URL. */
  @Transactional
  public String put(UUID organizerId, UUID eventId, byte[] data) {
    ownEvent(organizerId, eventId);
    return store(eventId, data);
  }

  /** The same, for an event the server itself owns (the demo catalogue): no organizer check. */
  @Transactional
  public String store(UUID eventId, byte[] data) {
    if (data.length > EventImages.MAX_BYTES) {
      throw new EventImageInvalidException("The photo is over 2 MB", true);
    }
    String type = EventImages.detectType(data)
        .orElseThrow(() -> new EventImageInvalidException("The photo has to be a JPEG, PNG or WebP image", false));
    imageRepository.save(new EventImage(eventId, type, data));
    // Millisecond precision: the URL's version has to survive a round trip through the database unchanged.
    LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    eventRepository.setImageUpdatedAt(eventId, now);
    return Event.imageUrl(eventId, now);
  }

  @Transactional
  public void delete(UUID organizerId, UUID eventId) {
    ownEvent(organizerId, eventId);
    if (imageRepository.existsById(eventId)) {
      imageRepository.deleteById(eventId);
    }
    eventRepository.setImageUpdatedAt(eventId, null);
  }

  public Optional<EventImage> get(UUID eventId) {
    return imageRepository.findById(eventId);
  }

  private Event ownEvent(UUID organizerId, UUID eventId) {
    return eventRepository.findByIdAndOrganizerId(eventId, organizerId)
        .orElseThrow(() -> new EventNotFoundException(String.format("Event with ID '%s' does not exist", eventId)));
  }
}
