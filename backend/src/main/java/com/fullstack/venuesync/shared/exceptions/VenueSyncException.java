package com.fullstack.venuesync.shared.exceptions;

public class VenueSyncException extends RuntimeException {

  public VenueSyncException() {
  }

  public VenueSyncException(String message) {
    super(message);
  }

  public VenueSyncException(String message, Throwable cause) {
    super(message, cause);
  }

  public VenueSyncException(Throwable cause) {
    super(cause);
  }

  public VenueSyncException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
