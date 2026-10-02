package com.fullstack.venuesync.events.domain;

/** Whether tickets for an event can be bought right now. Computed on the server, never on the client's clock. */
public enum SalesStatus {
  UPCOMING,
  ON_SALE,
  ENDED
}
