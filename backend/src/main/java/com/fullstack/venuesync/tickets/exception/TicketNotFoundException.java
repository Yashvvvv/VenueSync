package com.fullstack.venuesync.tickets.exception;

import com.fullstack.venuesync.shared.exceptions.VenueSyncException;

public class TicketNotFoundException extends VenueSyncException {

  public TicketNotFoundException() {
  }

  public TicketNotFoundException(String message) {
    super(message);
  }

  public TicketNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public TicketNotFoundException(Throwable cause) {
    super(cause);
  }

  public TicketNotFoundException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
