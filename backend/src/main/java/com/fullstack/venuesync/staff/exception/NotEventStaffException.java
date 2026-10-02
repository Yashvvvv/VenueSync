package com.fullstack.venuesync.staff.exception;

import com.fullstack.venuesync.shared.exceptions.VenueSyncException;

/** The user is neither the organizer nor on the door staff of the event they tried to act on. */
public class NotEventStaffException extends VenueSyncException {

  public NotEventStaffException(String message) {
    super(message);
  }
}
