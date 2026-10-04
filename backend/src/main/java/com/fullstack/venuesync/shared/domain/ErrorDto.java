package com.fullstack.venuesync.shared.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorDto {

  /**
   * Stable, machine-readable error code (e.g. {@code TICKETS_SOLD_OUT}). Clients branch on this,
   * never on {@link #error}, which is human-readable text that may change.
   */
  private String code;

  private String error;

  /** The request field the error is about, when there is one, so a form can mark it. Omitted otherwise. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String field;

  public ErrorDto(String code, String error) {
    this(code, error, null);
  }
}
