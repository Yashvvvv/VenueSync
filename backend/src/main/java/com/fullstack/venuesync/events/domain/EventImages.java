package com.fullstack.venuesync.events.domain;

import java.util.Map;
import java.util.Optional;

/** What counts as an event photo: JPEG, PNG or WebP up to 2 MB, and the bytes must really be that format. */
public final class EventImages {

  public static final int MAX_BYTES = 2 * 1024 * 1024;

  private static final Map<String, byte[]> SIGNATURES = Map.of(
      "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
      "image/png", new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A},
      "image/webp", new byte[] {'R', 'I', 'F', 'F'});

  private EventImages() {}

  /**
   * The content type to store and serve, from the bytes themselves: a declared type is only a claim, and serving
   * something else under an image type is how files sneak past a browser.
   */
  public static Optional<String> detectType(byte[] data) {
    if (data == null || data.length < 12) {
      return Optional.empty();
    }
    for (Map.Entry<String, byte[]> entry : SIGNATURES.entrySet()) {
      if (startsWith(data, entry.getValue())
          && (!entry.getKey().equals("image/webp") || (data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P'))) {
        return Optional.of(entry.getKey());
      }
    }
    return Optional.empty();
  }

  private static boolean startsWith(byte[] data, byte[] prefix) {
    for (int i = 0; i < prefix.length; i++) {
      if (data[i] != prefix[i]) {
        return false;
      }
    }
    return true;
  }
}
