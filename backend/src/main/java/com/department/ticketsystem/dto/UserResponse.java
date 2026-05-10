package com.department.ticketsystem.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String firebaseUid,
        String name,
        String email,
        String role,
        String department,
        LocalDateTime createdAt,
        boolean profileComplete
) {
}
