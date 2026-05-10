package com.department.ticketsystem.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventRequest(
        @NotBlank String name,
        @NotBlank String department,
        @NotNull LocalDateTime dateTime,
        @NotBlank String venue,
        @NotNull @DecimalMin("0.0") BigDecimal ticketPrice,
        @NotNull @Min(1) Integer availableTickets
) {
}
