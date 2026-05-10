package com.department.ticketsystem.dto;

import java.math.BigDecimal;

public record RevenuePointResponse(
        Long eventId,
        String eventName,
        BigDecimal revenue,
        long bookings,
        long ticketsSold
) {
}
