package com.department.ticketsystem.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String message,
        String type,
        boolean readStatus,
        LocalDateTime timestamp
) {
}
