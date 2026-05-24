package com.department.ticketsystem.service;

import com.department.ticketsystem.dto.EventRequest;
import com.department.ticketsystem.dto.EventResponse;
import com.department.ticketsystem.model.Booking;
import com.department.ticketsystem.model.BookingStatus;
import com.department.ticketsystem.model.Department;
import com.department.ticketsystem.model.Event;
import com.department.ticketsystem.model.NotificationType;
import com.department.ticketsystem.model.Seat;
import com.department.ticketsystem.model.SeatStatus;
import com.department.ticketsystem.model.User;
import com.department.ticketsystem.repository.BookingRepository;
import com.department.ticketsystem.repository.EventRepository;
import com.department.ticketsystem.repository.SeatRepository;
import com.department.ticketsystem.repository.UserRepository;
import com.department.ticketsystem.util.SeatNumberComparator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final PricingService pricingService;
    private final NotificationService notificationService;
    private final EmailService emailService;

    public EventService(EventRepository eventRepository, BookingRepository bookingRepository, SeatRepository seatRepository,
                        UserRepository userRepository, PricingService pricingService,
                        NotificationService notificationService, EmailService emailService) {
        this.eventRepository = eventRepository;
        this.bookingRepository = bookingRepository;
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
        this.pricingService = pricingService;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    @Transactional
    public List<EventResponse> getAllEventsForUser(String email) {
        User user = getUser(email);
        List<Event> events = user.getRole().name().equals("ADMIN")
                ? eventRepository.findAll()
                : eventRepository.findAllByOrderByDateTimeAsc();
        Map<Long, SeatCountSnapshot> seatCounts = loadSeatCounts();
        return events.stream()
                .map(event -> toResponse(event, seatCounts.getOrDefault(event.getId(), SeatCountSnapshot.empty())))
                .toList();
    }

    @Transactional
    public List<EventResponse> getAllEventsForAdmin() {
        Map<Long, SeatCountSnapshot> seatCounts = loadSeatCounts();
        return eventRepository.findAll().stream()
                .map(event -> toResponse(event, seatCounts.getOrDefault(event.getId(), SeatCountSnapshot.empty())))
                .toList();
    }

    @Transactional
    public EventResponse getEvent(Long id, String email) {
        Event event = getAccessibleEventEntity(id, email);
        ensureSeatsForEvent(event);
        refreshAvailableTickets(event);
        return toResponse(event);
    }

    @Transactional
    public EventResponse createEvent(EventRequest request) {
        Event event = new Event();
        event.setName(request.name());
        event.setDepartment(Department.fromValue(request.department()).name());
        event.setDateTime(request.dateTime());
        event.setVenue(request.venue());
        event.setTicketPrice(request.ticketPrice());
        event.setAvailableTickets(request.availableTickets());
        event.setTotalTickets(request.availableTickets());
        Event saved = eventRepository.save(event);
        ensureSeatsForEvent(saved);
        refreshAvailableTickets(saved);
        notifyEligibleUsersForNewEvent(saved);
        return toResponse(saved);
    }

    @Transactional
    public EventResponse updateEvent(Long id, EventRequest request) {
        Event event = getEventEntity(id);
        ensureSeatsForEvent(event);
        LocalDateTimeSnapshot previous = LocalDateTimeSnapshot.from(event);
        long lockedSeats = seatRepository.countByEventAndStatus(event, SeatStatus.BOOKED)
                + seatRepository.countByEventAndStatus(event, SeatStatus.HELD);
        if (request.availableTickets() < lockedSeats) {
            throw new IllegalArgumentException("Capacity cannot be less than booked tickets");
        }
        event.setName(request.name());
        event.setDepartment(Department.fromValue(request.department()).name());
        event.setDateTime(request.dateTime());
        event.setVenue(request.venue());
        event.setTicketPrice(request.ticketPrice());
        event.setTotalTickets(request.availableTickets());
        Event saved = eventRepository.save(event);
        syncSeatCapacity(saved);
        refreshAvailableTickets(saved);
        notifyBookedUsersForEventUpdates(saved, previous, request);
        return toResponse(saved);
    }

    @Transactional
    public void deleteEvent(Long id) {
        Event event = getEventEntity(id);

        List<Booking> bookings = bookingRepository.findByEventOrderByBookingDateDesc(event);
        List<Booking> confirmedBookings = bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
                .toList();

        for (Booking booking : confirmedBookings) {
            notificationService.createNotification(
                    booking.getUser(),
                    NotificationType.EVENT_UPDATE,
                    buildCancellationMessage(event, booking));
            emailService.sendEventUpdateEmail(List.of(booking.getUser()), event, buildCancellationMessage(event, booking));
        }

        seatRepository.deleteByEvent(event);
        bookingRepository.deleteByEvent(event);
        eventRepository.delete(event);
    }

    public Event getEventEntity(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
    }

    public List<Event> getEventEntities() {
        return eventRepository.findAll();
    }

    @Transactional
    public List<com.department.ticketsystem.dto.SeatResponse> getEventSeats(Long id, String email) {
        Event event = getAccessibleEventEntity(id, email);
        ensureSeatsForEvent(event);
        return seatRepository.findByEventOrderBySeatNumberAsc(event).stream()
                .sorted(SeatNumberComparator.BY_SEAT_NUMBER)
                .map(seat -> new com.department.ticketsystem.dto.SeatResponse(
                        seat.getId(),
                        seat.getSeatNumber(),
                        seat.getStatus().name(),
                        seat.getHeldBy() != null && seat.getHeldBy().getEmail().equals(email)))
                .toList();
    }

    public EventResponse toResponse(Event event) {
        long heldSeats = seatRepository.countByEventAndStatus(event, SeatStatus.HELD);
        int availableTickets = refreshAvailableTickets(event);
        int sellableAvailableSeats = availableTickets + (int) heldSeats;
        int bookedSeats = Math.max(0, event.getTotalTickets() - availableTickets - (int) heldSeats);
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getDepartment(),
                event.getDateTime(),
                event.getVenue(),
                event.getTicketPrice(),
                pricingService.calculateCurrentPrice(event, sellableAvailableSeats),
                pricingService.calculateMultiplier(event, sellableAvailableSeats),
                availableTickets,
                event.getTotalTickets(),
                bookedSeats,
                heldSeats);
    }

    private EventResponse toResponse(Event event, SeatCountSnapshot seatCounts) {
        int availableTickets = seatCounts.available() == 0 && seatCounts.total() == 0
                ? event.getAvailableTickets()
                : (int) seatCounts.available();
        long heldSeats = seatCounts.held();
        int sellableAvailableSeats = availableTickets + (int) heldSeats;
        int bookedSeats = Math.toIntExact(seatCounts.booked());
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getDepartment(),
                event.getDateTime(),
                event.getVenue(),
                event.getTicketPrice(),
                pricingService.calculateCurrentPrice(event, sellableAvailableSeats),
                pricingService.calculateMultiplier(event, sellableAvailableSeats),
                availableTickets,
                event.getTotalTickets(),
                bookedSeats,
                heldSeats);
    }

    private Map<Long, SeatCountSnapshot> loadSeatCounts() {
        Map<Long, SeatCountAccumulator> counts = new HashMap<>();
        for (Object[] row : seatRepository.countSeatsByEventAndStatus()) {
            Long eventId = (Long) row[0];
            SeatStatus status = (SeatStatus) row[1];
            long count = ((Number) row[2]).longValue();
            counts.computeIfAbsent(eventId, ignored -> new SeatCountAccumulator()).add(status, count);
        }
        Map<Long, SeatCountSnapshot> snapshots = new HashMap<>();
        counts.forEach((eventId, accumulator) -> snapshots.put(eventId, accumulator.toSnapshot()));
        return snapshots;
    }

    @Transactional
    public void ensureSeatsForEvent(Event event) {
        List<Seat> existingSeats = seatRepository.findByEventOrderBySeatNumberAsc(event);
        if (existingSeats.size() >= event.getTotalTickets()) {
            return;
        }
        List<Seat> newSeats = new ArrayList<>();
        for (int index = existingSeats.size() + 1; index <= event.getTotalTickets(); index++) {
            Seat seat = new Seat();
            seat.setEvent(event);
            seat.setSeatNumber(buildSeatNumber(index));
            seat.setStatus(SeatStatus.AVAILABLE);
            newSeats.add(seat);
        }
        seatRepository.saveAll(newSeats);
    }

    @Transactional
    public void syncSeatCapacity(Event event) {
        ensureSeatsForEvent(event);
        List<Seat> seats = seatRepository.findByEventOrderBySeatNumberAsc(event).stream()
                .sorted(SeatNumberComparator.BY_SEAT_NUMBER)
                .toList();
        if (seats.size() <= event.getTotalTickets()) {
            return;
        }
        long removable = seats.stream().filter(seat -> seat.getStatus() == SeatStatus.AVAILABLE).count();
        int overflow = seats.size() - event.getTotalTickets();
        if (removable < overflow) {
            throw new IllegalArgumentException("Reduce held or booked seats before lowering capacity");
        }
        List<Seat> toDelete = seats.stream()
                .filter(seat -> seat.getStatus() == SeatStatus.AVAILABLE)
                .sorted(SeatNumberComparator.BY_SEAT_NUMBER.reversed())
                .limit(overflow)
                .toList();
        seatRepository.deleteAll(toDelete);
    }

    @Transactional
    public int refreshAvailableTickets(Event event) {
        int available = (int) seatRepository.countByEventAndStatus(event, SeatStatus.AVAILABLE);
        event.setAvailableTickets(available);
        eventRepository.save(event);
        return available;
    }

    public Event getAccessibleEventEntity(Long id, String email) {
        Event event = getEventEntity(id);
        getUser(email);
        return event;
    }

    private User getUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRole().name().equals("USER")
                && (user.getDepartment() == null
                || user.getDepartment().isBlank()
                || user.getDepartment().equalsIgnoreCase("Unassigned"))) {
            throw new IllegalArgumentException("Complete your profile before accessing events");
        }
        return user;
    }

    private String buildSeatNumber(int index) {
        int row = (index - 1) / 10;
        int seat = ((index - 1) % 10) + 1;
        return String.valueOf((char) ('A' + row)) + seat;
    }

    private String buildCancellationMessage(Event event, Booking booking) {
        String seatDetails = booking.getSeatNumbers() == null || booking.getSeatNumbers().isBlank()
                ? "No seat numbers assigned"
                : booking.getSeatNumbers();
        return "Event cancelled: " + event.getName()
                + " | Tickets booked: " + booking.getTickets()
                + " (" + seatDetails + ")"
                + " | Amount refunded: Rs. " + booking.getTotalAmount();
    }

    private void notifyBookedUsersForEventUpdates(Event event, LocalDateTimeSnapshot previous, EventRequest request) {
        List<String> changes = new ArrayList<>();
        if (!Objects.equals(previous.dateTime(), request.dateTime())) {
            changes.add("Date/time changed");
        }
        if (!Objects.equals(previous.venue(), request.venue())) {
            changes.add("Venue changed from " + previous.venue() + " to " + request.venue());
        }
        if (moneyChanged(previous.ticketPrice(), request.ticketPrice())) {
            changes.add("Ticket price changed from Rs. " + previous.ticketPrice() + " to Rs. " + request.ticketPrice());
        }
        if (changes.isEmpty()) {
            return;
        }

        String updateSummary = String.join("; ", changes);
        List<User> bookedUsers = bookingRepository.findByEventAndStatusOrderByBookingDateDesc(event, BookingStatus.CONFIRMED)
                .stream()
                .map(Booking::getUser)
                .filter(user -> user.getEmail() != null && !user.getEmail().isBlank())
                .distinct()
                .toList();

        for (User user : bookedUsers) {
            notificationService.createNotification(user, NotificationType.EVENT_UPDATE,
                    "Event updated: " + event.getName() + " | " + updateSummary);
        }
        emailService.sendEventUpdateEmail(bookedUsers, event, updateSummary);
    }

    private void notifyEligibleUsersForNewEvent(Event event) {
        String message = "New event added: " + event.getName() + " at " + event.getVenue() + ".";
        List<User> eligibleUsers = userRepository.findAll().stream()
                .filter(user -> user.getRole().name().equals("USER"))
                .filter(user -> user.getEmail() != null && !user.getEmail().isBlank())
                .toList();
        for (User user : eligibleUsers) {
            notificationService.createNotification(user, NotificationType.EVENT_UPDATE, message);
        }
        emailService.sendNotificationEmail(eligibleUsers, "New event added: " + event.getName(), "New Event Added", message);
    }

    private boolean moneyChanged(BigDecimal previous, BigDecimal current) {
        if (previous == null || current == null) {
            return !Objects.equals(previous, current);
        }
        return previous.compareTo(current) != 0;
    }

    private record LocalDateTimeSnapshot(java.time.LocalDateTime dateTime, String venue, BigDecimal ticketPrice) {
        private static LocalDateTimeSnapshot from(Event event) {
            return new LocalDateTimeSnapshot(event.getDateTime(), event.getVenue(), event.getTicketPrice());
        }
    }

    private record SeatCountSnapshot(long available, long held, long booked) {
        private static SeatCountSnapshot empty() {
            return new SeatCountSnapshot(0, 0, 0);
        }

        private long total() {
            return available + held + booked;
        }
    }

    private static class SeatCountAccumulator {
        private long available;
        private long held;
        private long booked;

        private void add(SeatStatus status, long count) {
            if (status == SeatStatus.AVAILABLE) {
                available += count;
            } else if (status == SeatStatus.HELD) {
                held += count;
            } else if (status == SeatStatus.BOOKED) {
                booked += count;
            }
        }

        private SeatCountSnapshot toSnapshot() {
            return new SeatCountSnapshot(available, held, booked);
        }
    }
}
