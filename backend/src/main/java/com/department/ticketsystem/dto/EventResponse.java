package com.department.ticketsystem.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String name,
        String department,
        LocalDateTime dateTime,
        String venue,
        BigDecimal ticketPrice,
        BigDecimal currentTicketPrice,
        BigDecimal pricingMultiplier,
        Integer availableTickets,
        Integer totalTickets,
        Integer soldTickets,
        long heldSeats
) {
}
