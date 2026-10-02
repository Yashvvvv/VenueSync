package com.fullstack.venuesync.events.exception;

import com.fullstack.venuesync.events.domain.SalesStatus;
import com.fullstack.venuesync.shared.exceptions.VenueSyncException;
import lombok.Getter;

/** Thrown when a purchase is attempted while the event's {@link SalesStatus} is not ON_SALE. */
@Getter
public class SalesPeriodException extends VenueSyncException {

  private final SalesStatus salesStatus;

  public SalesPeriodException(SalesStatus salesStatus) {
    super(salesStatus == SalesStatus.UPCOMING ? "Ticket sales have not started yet" : "Ticket sales have ended");
    this.salesStatus = salesStatus;
  }
}
