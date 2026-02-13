package com.fullstack.venuesync.events.exception;

import com.fullstack.venuesync.shared.exceptions.VenueSyncException;

public class SalesPeriodException extends VenueSyncException {

  public SalesPeriodException() {
  }

  public SalesPeriodException(String message) {
    super(message);
  }

  public SalesPeriodException(String message, Throwable cause) {
    super(message, cause);
  }

  public SalesPeriodException(Throwable cause) {
    super(cause);
  }

  public SalesPeriodException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
