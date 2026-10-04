package com.fullstack.venuesync.shared.exceptions;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.fullstack.venuesync.events.domain.SalesStatus;
import com.fullstack.venuesync.shared.domain.ErrorDto;
import com.fullstack.venuesync.events.exception.EventNotFoundException;
import com.fullstack.venuesync.events.exception.CapacityBelowSoldException;
import com.fullstack.venuesync.events.exception.EventHasSalesException;
import com.fullstack.venuesync.events.exception.TicketTypeHasSalesException;
import com.fullstack.venuesync.events.exception.EventUpdateException;
import com.fullstack.venuesync.events.exception.SalesPeriodException;
import com.fullstack.venuesync.tickets.exception.IdempotencyKeyReusedException;
import com.fullstack.venuesync.tickets.exception.TicketNotFoundException;
import com.fullstack.venuesync.staff.exception.NotEventStaffException;
import com.fullstack.venuesync.staff.exception.StaffInviteExpiredException;
import com.fullstack.venuesync.staff.exception.StaffInviteNotFoundException;
import com.fullstack.venuesync.staff.exception.StaffInviteUsedException;
import com.fullstack.venuesync.tickets.exception.TicketTypeNotFoundException;
import com.fullstack.venuesync.tickets.exception.TicketsSoldOutException;
import com.fullstack.venuesync.validation.exception.QrCodeGenerationException;
import com.fullstack.venuesync.validation.exception.QrCodeNotFoundException;

/**
 * Every error leaves the API as {@code {code, error}} with a status that says whose fault it is:
 * 404 missing, 409 state conflict (sold out, not on sale), 4xx client mistakes, 5xx ours.
 * The {@code code} values are a public contract — clients switch on them — so never rename one.
 *
 * <p>Extends Spring's {@link ResponseEntityExceptionHandler} so framework errors (malformed JSON,
 * non-UUID path ids, missing headers, unknown URLs, wrong HTTP method) keep their proper 4xx
 * status instead of falling into the catch-all 500 — only the body is rewritten to {@link ErrorDto}.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(TicketNotFoundException.class)
  public ResponseEntity<ErrorDto> handleTicketNotFoundException(TicketNotFoundException ex) {
    return respond(HttpStatus.NOT_FOUND, "TICKET_NOT_FOUND", "Ticket not found", ex);
  }

  @ExceptionHandler(TicketsSoldOutException.class)
  public ResponseEntity<ErrorDto> handleTicketsSoldOutException(TicketsSoldOutException ex) {
    return respond(HttpStatus.CONFLICT, "TICKETS_SOLD_OUT", "Tickets are sold out for this ticket type", ex);
  }

  @ExceptionHandler(SalesPeriodException.class)
  public ResponseEntity<ErrorDto> handleSalesPeriodException(SalesPeriodException ex) {
    String code = ex.getSalesStatus() == SalesStatus.UPCOMING ? "SALES_NOT_STARTED" : "SALES_ENDED";
    return respond(HttpStatus.CONFLICT, code, ex.getMessage(), ex);
  }

  @ExceptionHandler(IdempotencyKeyReusedException.class)
  public ResponseEntity<ErrorDto> handleIdempotencyKeyReusedException(IdempotencyKeyReusedException ex) {
    return respond(HttpStatus.UNPROCESSABLE_ENTITY, "IDEMPOTENCY_KEY_REUSED",
        "This Idempotency-Key was already used for a different purchase", ex);
  }

  @ExceptionHandler(QrCodeNotFoundException.class)
  public ResponseEntity<ErrorDto> handleQrCodeNotFoundException(QrCodeNotFoundException ex) {
    return respond(HttpStatus.NOT_FOUND, "QR_CODE_NOT_FOUND", "QR code not found", ex);
  }

  @ExceptionHandler(QrCodeGenerationException.class)
  public ResponseEntity<ErrorDto> handleQrCodeGenerationException(QrCodeGenerationException ex) {
    return respond(HttpStatus.INTERNAL_SERVER_ERROR, "QR_CODE_ERROR", "Unable to generate QR Code", ex);
  }

  @ExceptionHandler(EventUpdateException.class)
  public ResponseEntity<ErrorDto> handleEventUpdateException(EventUpdateException ex) {
    return respond(HttpStatus.BAD_REQUEST, "EVENT_UPDATE_INVALID", "Unable to update event", ex);
  }

  @ExceptionHandler(NotEventStaffException.class)
  public ResponseEntity<ErrorDto> handleNotEventStaffException(NotEventStaffException ex) {
    return respond(HttpStatus.FORBIDDEN, "NOT_EVENT_STAFF", "You are not door staff for this event", ex);
  }

  @ExceptionHandler(StaffInviteNotFoundException.class)
  public ResponseEntity<ErrorDto> handleStaffInviteNotFoundException(StaffInviteNotFoundException ex) {
    return respond(HttpStatus.NOT_FOUND, "INVITE_NOT_FOUND", "Invite code not found", ex);
  }

  @ExceptionHandler(StaffInviteUsedException.class)
  public ResponseEntity<ErrorDto> handleStaffInviteUsedException(StaffInviteUsedException ex) {
    return respond(HttpStatus.CONFLICT, "INVITE_USED", "This invite code has already been used", ex);
  }

  @ExceptionHandler(StaffInviteExpiredException.class)
  public ResponseEntity<ErrorDto> handleStaffInviteExpiredException(StaffInviteExpiredException ex) {
    return respond(HttpStatus.GONE, "INVITE_EXPIRED", "This invite code has expired", ex);
  }

  @ExceptionHandler(TicketTypeNotFoundException.class)
  public ResponseEntity<ErrorDto> handleTicketTypeNotFoundException(TicketTypeNotFoundException ex) {
    return respond(HttpStatus.NOT_FOUND, "TICKET_TYPE_NOT_FOUND", "Ticket type not found", ex);
  }

  /** Removing a ticket type someone bought would delete their ticket. */
  @ExceptionHandler(TicketTypeHasSalesException.class)
  public ResponseEntity<ErrorDto> handleTicketTypeHasSales(TicketTypeHasSalesException ex) {
    return respond(HttpStatus.CONFLICT, "TICKET_TYPE_HAS_SALES",
        "This ticket type has tickets issued, so it can't be removed", ex);
  }

  /** A capacity below what's issued would make the event "oversold" on paper. */
  @ExceptionHandler(CapacityBelowSoldException.class)
  public ResponseEntity<ErrorDto> handleCapacityBelowSold(CapacityBelowSoldException ex) {
    return respond(HttpStatus.CONFLICT, "CAPACITY_BELOW_SOLD",
        "Capacity can't be lower than the tickets already issued", ex);
  }

  /** Deleting an event with tickets would delete them: it has to be cancelled instead. */
  @ExceptionHandler(EventHasSalesException.class)
  public ResponseEntity<ErrorDto> handleEventHasSales(EventHasSalesException ex) {
    return respond(HttpStatus.CONFLICT, "EVENT_HAS_SALES",
        "This event has tickets issued, so it can't be deleted. Cancel it instead", ex);
  }

  @ExceptionHandler(EventNotFoundException.class)
  public ResponseEntity<ErrorDto> handleEventNotFoundException(EventNotFoundException ex) {
    return respond(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "Event not found", ex);
  }

  /** Users are provisioned on their first authenticated request, so a missing one is our bug. */
  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ErrorDto> handleUserNotFoundException(UserNotFoundException ex) {
    return respond(HttpStatus.INTERNAL_SERVER_ERROR, "USER_NOT_PROVISIONED", "User not found", ex);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .findFirst()
        .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
        .orElse("Validation error occurred");
    return new ResponseEntity<>(errorBody(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, ex), headers, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorDto> handleConstraintViolation(ConstraintViolationException ex) {
    String message = ex.getConstraintViolations().stream()
        .findFirst()
        .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
        .orElse("Constraint violation occurred");
    return respond(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, ex);
  }

  /**
   * A unique constraint fired: two requests raced to create the same thing (e.g. a double-submitted create with one
   * idempotency key). The first one won; this one is a conflict, not a server error.
   */
  @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
  public ResponseEntity<ErrorDto> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
    return respond(HttpStatus.CONFLICT, "CONFLICT", "This was changed by another request. Refresh and try again", ex);
  }

  /** Every other Spring MVC exception: keep Spring's status, return our body. */
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    String code = switch (status.value()) {
      case 404 -> "NOT_FOUND";
      case 405 -> "METHOD_NOT_ALLOWED";
      case 415 -> "UNSUPPORTED_MEDIA_TYPE";
      default -> status.is5xxServerError() ? "INTERNAL_ERROR" : "INVALID_REQUEST";
    };
    String message = body instanceof ProblemDetail problem && problem.getDetail() != null
        ? problem.getDetail()
        : "Invalid request";
    return new ResponseEntity<>(errorBody(HttpStatus.valueOf(status.value()), code, message, ex), headers, status);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorDto> handleException(Exception ex) {
    return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unknown error occurred", ex);
  }

  private ResponseEntity<ErrorDto> respond(HttpStatus status, String code, String message, Exception ex) {
    return new ResponseEntity<>(errorBody(status, code, message, ex), status);
  }

  /** 5xx are our bugs: log with stack trace. 4xx are the client's: one warn line is enough. */
  private ErrorDto errorBody(HttpStatus status, String code, String message, Exception ex) {
    if (status.is5xxServerError()) {
      log.error("{} -> {} {}", ex.getClass().getSimpleName(), status.value(), code, ex);
    } else {
      log.warn("{} -> {} {}: {}", ex.getClass().getSimpleName(), status.value(), code, ex.getMessage());
    }
    return new ErrorDto(code, message);
  }
}
