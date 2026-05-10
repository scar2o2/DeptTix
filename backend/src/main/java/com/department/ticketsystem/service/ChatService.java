package com.department.ticketsystem.service;

import com.department.ticketsystem.model.Event;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private static final DateTimeFormatter EVENT_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

    private static final List<String> ALLOWED_KEYWORDS = List.of(
            "event", "events", "ticket", "tickets", "available", "availability", "seat", "seats", "left",
            "timing", "timings", "time", "date", "when", "schedule", "start", "venue", "location", "place",
            "where", "book", "booking", "price", "cost", "fee", "amount", "department", "dept", "branch",
            "info", "details", "summary", "list");
    private static final Set<String> EVENT_NAME_STOP_WORDS = Set.of(
            "event", "events", "the", "and", "for", "with", "2024", "2025", "2026");

    private final EventService eventService;

    public ChatService(EventService eventService) {
        this.eventService = eventService;
    }

    public String answer(String message) {
        String normalized = normalize(message);
        List<Event> events = eventService.getEventEntities().stream()
                .sorted(Comparator.comparing(Event::getDateTime))
                .toList();
        if (events.isEmpty()) {
            return "No events are available right now.";
        }

        Optional<Event> requestedEvent = findRequestedEvent(normalized, events);
        boolean eventRelated = ALLOWED_KEYWORDS.stream().anyMatch(normalized::contains) || requestedEvent.isPresent();
        if (!eventRelated) {
            return "I can only answer event-related questions about schedules, venues, prices, and ticket availability.";
        }

        Intent intent = detectIntent(normalized);

        if (requestedEvent.isPresent()) {
            return formatEventDetail(requestedEvent.get(), intent);
        }

        if (intent == Intent.SUMMARY && !asksForEventList(normalized)) {
            return "Please include an event name with what you need, like venue, timings, price, or available seats.";
        }

        return events.stream()
                .map(event -> formatEventDetail(event, intent))
                .reduce((left, right) -> left + " | " + right)
                .orElse("No matching event information is available right now.");
    }

    private Optional<Event> findRequestedEvent(String normalizedMessage, List<Event> events) {
        return events.stream()
                .map(event -> new EventMatch(event, eventNameMatchScore(normalizedMessage, event)))
                .filter(match -> match.score() > 0)
                .max(Comparator.comparingInt(EventMatch::score))
                .map(EventMatch::event);
    }

    private int eventNameMatchScore(String normalizedMessage, Event event) {
        String normalizedEventName = normalize(event.getName());
        if (normalizedEventName.isBlank()) {
            return 0;
        }
        if (normalizedMessage.contains(normalizedEventName)) {
            return 100 + normalizedEventName.length();
        }
        int score = 0;
        for (String token : normalizedEventName.split(" ")) {
            if (token.length() >= 4 && !EVENT_NAME_STOP_WORDS.contains(token) && normalizedMessage.contains(token)) {
                score += token.length();
            }
        }
        return score;
    }

    private Intent detectIntent(String normalized) {
        if (containsAny(normalized, "venue", "location", "place", "where")) {
            return Intent.VENUE;
        }
        if (containsAny(normalized, "timing", "timings", "time", "date", "when", "schedule", "start")) {
            return Intent.TIMING;
        }
        if (containsAny(normalized, "price", "cost", "fee", "amount")) {
            return Intent.PRICE;
        }
        if (containsAny(normalized, "available", "availability", "tickets left", "ticket left",
                "seats left", "seat left", "left", "ticket", "tickets", "seat", "seats", "book", "booking")) {
            return Intent.AVAILABILITY;
        }
        if (containsAny(normalized, "department", "dept", "branch")) {
            return Intent.DEPARTMENT;
        }
        return Intent.SUMMARY;
    }

    private record EventMatch(Event event, int score) {
    }

    private String formatEventDetail(Event event, Intent intent) {
        return switch (intent) {
            case VENUE -> event.getName() + " venue: " + emptyFallback(event.getVenue(), "Venue not announced yet");
            case TIMING -> event.getName() + " timing: " + event.getDateTime().format(EVENT_TIME_FORMAT);
            case AVAILABILITY -> event.getName() + ": " + event.getAvailableTickets() + " seats left";
            case PRICE -> event.getName() + " ticket price: Rs. " + event.getTicketPrice();
            case DEPARTMENT -> event.getName() + " department: " + event.getDepartment();
            case SUMMARY -> event.getName()
                    + " | Venue: " + emptyFallback(event.getVenue(), "Venue not announced yet")
                    + " | Timing: " + event.getDateTime().format(EVENT_TIME_FORMAT)
                    + " | Seats left: " + event.getAvailableTickets()
                    + " | Price: Rs. " + event.getTicketPrice();
        };
    }

    private boolean asksForEventList(String normalized) {
        return containsAny(normalized, "event", "events", "list", "available");
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String emptyFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String normalize(String value) {
        return NON_ALPHANUMERIC.matcher(value.toLowerCase(Locale.ENGLISH).trim()).replaceAll(" ").trim();
    }

    private enum Intent {
        VENUE,
        TIMING,
        AVAILABILITY,
        PRICE,
        DEPARTMENT,
        SUMMARY
    }
}
