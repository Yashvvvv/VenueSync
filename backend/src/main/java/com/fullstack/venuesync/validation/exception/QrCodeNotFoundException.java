package com.fullstack.venuesync.validation.exception;

import com.fullstack.venuesync.shared.exceptions.VenueSyncException;

public class QrCodeNotFoundException extends VenueSyncException {

  public QrCodeNotFoundException() {
  }

  public QrCodeNotFoundException(String message) {
    super(message);
  }

  public QrCodeNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public QrCodeNotFoundException(Throwable cause) {
    super(cause);
  }

  public QrCodeNotFoundException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
