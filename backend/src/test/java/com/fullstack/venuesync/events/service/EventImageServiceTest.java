package com.fullstack.venuesync.events.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.events.domain.EventImage;
import com.fullstack.venuesync.events.domain.EventImages;
import com.fullstack.venuesync.events.exception.EventImageInvalidException;
import com.fullstack.venuesync.events.exception.EventNotFoundException;
import com.fullstack.venuesync.events.repository.EventImageRepository;
import com.fullstack.venuesync.events.repository.EventRepository;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventImageServiceTest {

  @Mock private EventRepository eventRepository;
  @Mock private EventImageRepository imageRepository;
  @InjectMocks private EventImageService service;

  private final UUID organizerId = UUID.randomUUID();
  private final UUID eventId = UUID.randomUUID();

  static byte[] jpeg(int size) {
    byte[] data = new byte[size];
    data[0] = (byte) 0xFF;
    data[1] = (byte) 0xD8;
    data[2] = (byte) 0xFF;
    return data;
  }

  private Event ownEvent() {
    Event event = new Event();
    event.setId(eventId);
    when(eventRepository.findByIdAndOrganizerId(eventId, organizerId)).thenReturn(Optional.of(event));
    return event;
  }

  @Test
  void aPhotoIsStoredWithTheTypeItReallyIsAndTheEventGetsAVersionedUrl() {
    ownEvent();
    String url = service.put(organizerId, eventId, jpeg(500));
    ArgumentCaptor<EventImage> stored = ArgumentCaptor.forClass(EventImage.class);
    verify(imageRepository).save(stored.capture());
    assertEquals("image/jpeg", stored.getValue().getContentType());
    assertEquals(eventId, stored.getValue().getEventId());
    assertTrue(url.startsWith("/api/v1/event-images/" + eventId + "?v="));
    verify(eventRepository).setImageUpdatedAt(org.mockito.ArgumentMatchers.eq(eventId), any());
  }

  @Test
  void someoneElsesEventIsNotFound() {
    when(eventRepository.findByIdAndOrganizerId(eventId, organizerId)).thenReturn(Optional.empty());
    assertThrows(EventNotFoundException.class, () -> service.put(organizerId, eventId, jpeg(500)));
    verify(imageRepository, never()).save(any());
  }

  @Test
  void notAnImageOrTooBigIsRefusedAndSaysWhich() {
    ownEvent();
    byte[] html = "<html><script>alert(1)</script></html>".getBytes();
    EventImageInvalidException wrong = assertThrows(EventImageInvalidException.class, () -> service.put(organizerId, eventId, html));
    assertEquals(false, wrong.isTooLarge());
    EventImageInvalidException big =
        assertThrows(EventImageInvalidException.class, () -> service.put(organizerId, eventId, jpeg(EventImages.MAX_BYTES + 1)));
    assertTrue(big.isTooLarge());
    verify(imageRepository, never()).save(any());
  }

  @Test
  void removingClearsTheUrl() {
    ownEvent();
    when(imageRepository.existsById(eventId)).thenReturn(true);
    service.delete(organizerId, eventId);
    verify(imageRepository).deleteById(eventId);
    verify(eventRepository).setImageUpdatedAt(eventId, null);
  }

  @Test
  void formatsAreTakenFromTheBytesNotTheirClaim() {
    assertEquals(Optional.of("image/jpeg"), EventImages.detectType(jpeg(20)));
    byte[] png = Arrays.copyOf(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}, 20);
    assertEquals(Optional.of("image/png"), EventImages.detectType(png));
    byte[] webp = Arrays.copyOf("RIFF\0\0\0\0WEBPVP8 ".getBytes(), 20);
    assertEquals(Optional.of("image/webp"), EventImages.detectType(webp));
    byte[] wav = Arrays.copyOf("RIFF\0\0\0\0WAVEfmt ".getBytes(), 20);
    assertEquals(Optional.empty(), EventImages.detectType(wav));
    assertEquals(Optional.empty(), EventImages.detectType(new byte[3]));
    assertNotNull(EventImages.detectType(new byte[0]));
  }
}
