package com.fullstack.venuesync.events.exception;

import com.fullstack.venuesync.shared.exceptions.VenueSyncException;

public class EventNotFoundException extends VenueSyncException {

  public EventNotFoundException() {
  }

  public EventNotFoundException(String message) {
    super(message);
  }

  public EventNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public EventNotFoundException(Throwable cause) {
    super(cause);
  }

  public EventNotFoundException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
