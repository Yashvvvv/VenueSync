package com.fullstack.venuesync.shared.domain;

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
}
