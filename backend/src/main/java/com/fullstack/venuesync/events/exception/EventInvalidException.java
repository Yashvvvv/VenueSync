package com.fullstack.venuesync.events.exception;

import com.fullstack.venuesync.shared.exceptions.VenueSyncException;
import lombok.Getter;

/** An event that can't be scheduled or sold as given; {@link #field} is the request field to fix. */
@Getter
public class EventInvalidException extends VenueSyncException {
  private final String field;

  public EventInvalidException(String field, String message) {
    super(message);
    this.field = field;
  }
}
