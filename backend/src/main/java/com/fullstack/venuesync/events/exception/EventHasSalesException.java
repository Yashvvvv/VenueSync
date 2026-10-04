package com.fullstack.venuesync.events.exception;

import com.fullstack.venuesync.shared.exceptions.VenueSyncException;

public class EventHasSalesException extends VenueSyncException {
  public EventHasSalesException(String message) {
    super(message);
  }
}
