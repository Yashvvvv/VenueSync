package com.fullstack.venuesync.events.exception;

import com.fullstack.venuesync.shared.exceptions.VenueSyncException;

public class CapacityBelowSoldException extends VenueSyncException {
  public CapacityBelowSoldException(String message) {
    super(message);
  }
}
