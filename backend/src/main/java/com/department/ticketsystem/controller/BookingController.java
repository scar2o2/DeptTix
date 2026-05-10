package com.department.ticketsystem.controller;

import com.department.ticketsystem.dto.BookingRequest;
import com.department.ticketsystem.dto.BookingResponse;
import com.department.ticketsystem.service.BookingService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public BookingResponse createBooking(@Valid @RequestBody BookingRequest request, Principal principal) {
        return bookingService.createBooking(principal.getName(), request);
    }

    @PostMapping("/hold")
    public Map<String, Object> holdSeats(@RequestBody Map<String, Object> request, Principal principal) {
        return bookingService.holdSeats(
                principal.getName(),
                toLong(request.get("eventId")),
                toLongList(request.get("seatIds")));
    }

    @PatchMapping("/{id}/cancel")
    public BookingResponse cancelBooking(@PathVariable Long id, Principal principal) {
        return bookingService.cancelBooking(principal.getName(), id);
    }

    @GetMapping("/me")
    public List<BookingResponse> getMyBookings(Principal principal) {
        return bookingService.getUserBookings(principal.getName());
    }

    private Long toLong(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("eventId is required");
        }
        return ((Number) value).longValue();
    }

    private List<Long> toLongList(Object value) {
        if (!(value instanceof List<?>)) {
            throw new IllegalArgumentException("seatIds is required");
        }
        List<?> items = (List<?>) value;
        if (items.isEmpty()) {
            throw new IllegalArgumentException("seatIds is required");
        }
        List<Long> seatIds = new ArrayList<>();
        for (Object item : items) {
            if (!(item instanceof Number)) {
                throw new IllegalArgumentException("seatIds must contain numeric values");
            }
            seatIds.add(((Number) item).longValue());
        }
        return seatIds;
    }
}
