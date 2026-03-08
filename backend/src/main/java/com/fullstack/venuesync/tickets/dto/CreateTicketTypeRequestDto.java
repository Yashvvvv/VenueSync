package com.fullstack.venuesync.tickets.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateTicketTypeRequestDto {

  @NotBlank(message = "Ticket type name is required")
  @Size(min = 1, max = 100, message = "Ticket type name must be between 1 and 100 characters")
  private String name;

  @NotNull(message = "Price is required")
  @PositiveOrZero(message = "Price must be zero or greater")
  private Double price;

  @Size(max = 500, message = "Description must be at most 500 characters")
  private String description;

  @Min(value = 0, message = "Total available must be zero or greater")
  private Integer totalAvailable;
}
