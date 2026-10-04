package com.fullstack.venuesync.events.exception;

import com.fullstack.venuesync.shared.exceptions.VenueSyncException;

public class StatusChangeInvalidException extends VenueSyncException {
  public StatusChangeInvalidException(String message) {
    super(message);
  }
}
