package com.department.ticketsystem.controller;

import com.department.ticketsystem.dto.SeatResponse;
import com.department.ticketsystem.dto.EventResponse;
import com.department.ticketsystem.service.EventService;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<EventResponse> getEvents(Principal principal) {
        return eventService.getAllEventsForUser(principal.getName());
    }

    @GetMapping("/{id}")
    public EventResponse getEvent(@PathVariable Long id, Principal principal) {
        return eventService.getEvent(id, principal.getName());
    }

    @GetMapping("/{id}/seats")
    public List<SeatResponse> getSeats(@PathVariable Long id, Principal principal) {
        return eventService.getEventSeats(id, principal.getName());
    }
}
