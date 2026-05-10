package com.department.ticketsystem.dto;

import java.time.LocalDateTime;

public record SavedPassengerResponse(
        Long id,
        String name,
        Integer age,
        String gender,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
