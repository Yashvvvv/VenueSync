package com.fullstack.venuesync.events.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetEventDetailsTicketTypesResponseDto {

  private UUID id;
  private String name;
  private Double price;
  private String description;
  private Integer totalAvailable;
  /** Tickets issued so far (every status). Filled by the controller from one count query, not by the mapper. */
  private Long sold;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
