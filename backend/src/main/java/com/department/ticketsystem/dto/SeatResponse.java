package com.department.ticketsystem.dto;

public record SeatResponse(
        Long id,
        String seatNumber,
        String status,
        boolean heldByCurrentUser
) {
}
