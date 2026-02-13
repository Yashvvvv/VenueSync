package com.fullstack.venuesync.events.exception;

import com.fullstack.venuesync.shared.exceptions.VenueSyncException;

public class EventUpdateException extends VenueSyncException {

  public EventUpdateException() {
  }

  public EventUpdateException(String message) {
    super(message);
  }

  public EventUpdateException(String message, Throwable cause) {
    super(message, cause);
  }

  public EventUpdateException(Throwable cause) {
    super(cause);
  }

  public EventUpdateException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
