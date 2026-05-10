package com.department.ticketsystem.controller;

import com.department.ticketsystem.dto.BookingStatsResponse;
import com.department.ticketsystem.dto.EventRequest;
import com.department.ticketsystem.dto.EventResponse;
import com.department.ticketsystem.dto.RevenuePointResponse;
import com.department.ticketsystem.service.AdminService;
import com.department.ticketsystem.service.EventService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final EventService eventService;

    public AdminController(AdminService adminService, EventService eventService) {
        this.adminService = adminService;
        this.eventService = eventService;
    }

    @GetMapping("/revenue")
    public List<RevenuePointResponse> getRevenue() {
        return adminService.getRevenueData();
    }

    @GetMapping("/bookings/stats")
    public BookingStatsResponse getBookingStats() {
        return adminService.getBookingStats();
    }

    @GetMapping("/export-report/{eventId}")
    public ResponseEntity<byte[]> exportReport(@PathVariable Long eventId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=event-report-" + eventId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(adminService.exportReport(eventId));
    }

    @GetMapping("/events")
    public List<EventResponse> getEvents() {
        return eventService.getAllEventsForAdmin();
    }

    @PostMapping("/events")
    public EventResponse createEvent(@Valid @RequestBody EventRequest request) {
        return eventService.createEvent(request);
    }

    @PutMapping("/events/{id}")
    public EventResponse updateEvent(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return eventService.updateEvent(id, request);
    }

    @DeleteMapping("/events/{id}")
    public void deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
    }
}
