package com.fullstack.venuesync.tickets.exception;

import com.fullstack.venuesync.shared.exceptions.VenueSyncException;

/** The same Idempotency-Key was sent for a different ticket type: a client bug, never a retry. */
public class IdempotencyKeyReusedException extends VenueSyncException {
}
