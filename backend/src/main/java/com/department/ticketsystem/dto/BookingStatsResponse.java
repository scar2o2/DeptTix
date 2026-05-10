package com.department.ticketsystem.dto;

import java.util.List;
import java.util.Map;

public record BookingStatsResponse(
        List<Map<String, Object>> ticketDistribution,
        List<Map<String, Object>> bookingsOverTime,
        List<Map<String, Object>> soldVsRemaining
) {
}
