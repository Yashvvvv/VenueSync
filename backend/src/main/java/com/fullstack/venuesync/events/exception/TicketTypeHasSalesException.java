package com.fullstack.venuesync.events.exception;

import com.fullstack.venuesync.shared.exceptions.VenueSyncException;

public class TicketTypeHasSalesException extends VenueSyncException {
  public TicketTypeHasSalesException(String message) {
    super(message);
  }
}
