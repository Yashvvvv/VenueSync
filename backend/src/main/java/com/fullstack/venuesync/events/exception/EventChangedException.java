package com.fullstack.venuesync.events.exception;

import com.fullstack.venuesync.shared.exceptions.VenueSyncException;

/** The update was made from an older copy of the event: someone changed it since. */
public class EventChangedException extends VenueSyncException {
  public EventChangedException(String message) {
    super(message);
  }
}
