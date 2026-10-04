package com.fullstack.venuesync.events.controller;

import static com.fullstack.venuesync.shared.security.JwtUtil.parseUserId;

import com.fullstack.venuesync.events.domain.EventImages;
import com.fullstack.venuesync.events.exception.EventImageInvalidException;
import com.fullstack.venuesync.events.service.EventImageService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Event photos. The organizer sends the raw image bytes (Content-Type image/jpeg, image/png or image/webp): no
 * multipart, so both clients upload with one plain request. Anyone can read a photo by its event id.
 */
@RestController
@RequiredArgsConstructor
public class EventImageController {

  private final EventImageService imageService;

  @PutMapping(path = "/api/v1/events/{eventId}/image", consumes = {"image/jpeg", "image/png", "image/webp"})
  public ResponseEntity<Map<String, String>> put(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId, HttpServletRequest request) throws IOException {
    // At most one byte over the limit is read: a 100 MB body is refused without ever being held in memory.
    byte[] data;
    try (InputStream body = request.getInputStream()) {
      data = body.readNBytes(EventImages.MAX_BYTES + 1);
    }
    if (data.length > EventImages.MAX_BYTES) {
      throw new EventImageInvalidException("The photo is over 2 MB", true);
    }
    String url = imageService.put(parseUserId(jwt), eventId, data);
    return ResponseEntity.ok(Map.of("imageUrl", url));
  }

  @DeleteMapping("/api/v1/events/{eventId}/image")
  public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId) {
    imageService.delete(parseUserId(jwt), eventId);
    return ResponseEntity.noContent().build();
  }

  /** Cached for a year: the URL carries the photo's version, so a new photo is a new URL. */
  @GetMapping("/api/v1/event-images/{eventId}")
  public ResponseEntity<byte[]> get(@PathVariable UUID eventId) {
    return imageService.get(eventId)
        .map(image -> ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.getContentType()))
            .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
            .header("X-Content-Type-Options", "nosniff")
            .body(image.getData()))
        .orElse(ResponseEntity.notFound().build());
  }
}
