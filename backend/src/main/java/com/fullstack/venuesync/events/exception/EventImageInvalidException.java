package com.fullstack.venuesync.events.exception;

import com.fullstack.venuesync.shared.exceptions.VenueSyncException;

/** Not a photo this API takes: not JPEG, PNG or WebP, empty, or over the size limit ({@link #isTooLarge}). */
public class EventImageInvalidException extends VenueSyncException {
  private final boolean tooLarge;

  public EventImageInvalidException(String message, boolean tooLarge) {
    super(message);
    this.tooLarge = tooLarge;
  }

  public boolean isTooLarge() {
    return tooLarge;
  }
}
