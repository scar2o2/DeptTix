package com.department.ticketsystem.service;

import com.department.ticketsystem.dto.BookingRequest;
import com.department.ticketsystem.dto.BookingResponse;
import com.department.ticketsystem.dto.TicketHolderDetails;
import com.department.ticketsystem.model.Booking;
import com.department.ticketsystem.model.BookingStatus;
import com.department.ticketsystem.model.Event;
import com.department.ticketsystem.model.NotificationType;
import com.department.ticketsystem.model.Role;
import com.department.ticketsystem.model.Seat;
import com.department.ticketsystem.model.SeatStatus;
import com.department.ticketsystem.model.User;
import com.department.ticketsystem.repository.BookingRepository;
import com.department.ticketsystem.repository.SeatRepository;
import com.department.ticketsystem.repository.UserRepository;
import com.department.ticketsystem.util.SeatNumberComparator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class BookingService {

    private static final int HOLD_MINUTES = 10;
    private static final int MAX_TICKETS_PER_BOOKING = 8;

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final EventService eventService;
    private final SeatRepository seatRepository;
    private final PricingService pricingService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    public BookingService(BookingRepository bookingRepository, UserRepository userRepository, EventService eventService,
                          SeatRepository seatRepository, PricingService pricingService, NotificationService notificationService,
                          EmailService emailService, ObjectMapper objectMapper) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.eventService = eventService;
        this.seatRepository = seatRepository;
        this.pricingService = pricingService;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public BookingResponse createBooking(String email, BookingRequest request) {
        User user = getUser(email);
        Event event = eventService.getAccessibleEventEntity(request.getEventId(), email);
        eventService.ensureSeatsForEvent(event);
        releaseExpiredHolds(event);

        int ticketCount = resolveTicketCount(request);
        if (ticketCount <= 0) {
            throw new IllegalArgumentException("Please select at least one seat");
        }
        if (ticketCount > MAX_TICKETS_PER_BOOKING) {
            throw new IllegalArgumentException("You can book a maximum of 8 tickets for any event");
        }

        List<TicketHolderDetails> ticketHolders = normalizeTicketHolders(request.getTicketHolders(), ticketCount);

        List<Seat> chosenSeats = resolveSeatsForBooking(event, request.getSeatIds(), ticketCount, user);
        if (chosenSeats.size() != ticketCount) {
            throw new IllegalArgumentException("Unable to reserve the requested number of seats");
        }

        return finalizeBooking(event, user, chosenSeats, ticketHolders, NotificationType.BOOKING_CONFIRMATION,
                buildBookingNotificationMessage(event.getName(), ticketHolders));
    }

    public List<BookingResponse> getUserBookings(String email) {
        User user = getAccount(email);
        return bookingRepository.findByUserOrderByBookingDateDesc(user).stream().map(this::toResponse).toList();
    }

    @Transactional
    public Map<String, Object> holdSeats(String email, Long eventId, List<Long> seatIds) {
        User user = getUser(email);
        Event event = eventService.getAccessibleEventEntity(eventId, email);
        eventService.ensureSeatsForEvent(event);
        releaseExpiredHolds(event);
        if (seatIds.size() > MAX_TICKETS_PER_BOOKING) {
            throw new IllegalArgumentException("You can book a maximum of 8 tickets for any event");
        }

        List<Seat> seats = seatRepository.findByEventAndIdIn(event, seatIds).stream()
                .sorted(SeatNumberComparator.BY_SEAT_NUMBER)
                .toList();
        if (seats.size() != seatIds.size()) {
            throw new IllegalArgumentException("One or more selected seats do not exist");
        }

        for (Seat seat : seats) {
            boolean heldByCurrentUser = seat.getStatus() == SeatStatus.HELD
                    && seat.getHeldBy() != null
                    && Objects.equals(seat.getHeldBy().getId(), user.getId());
            if (seat.getStatus() == SeatStatus.BOOKED || (seat.getStatus() == SeatStatus.HELD && !heldByCurrentUser)) {
                throw new IllegalArgumentException("Some selected seats are no longer available");
            }
        }

        LocalDateTime heldAt = LocalDateTime.now();
        seats.forEach(seat -> {
            seat.setStatus(SeatStatus.HELD);
            seat.setHeldBy(user);
            seat.setHeldAt(heldAt);
        });
        seatRepository.saveAll(seats);
        eventService.refreshAvailableTickets(event);

        int heldSeats = (int) seatRepository.countByEventAndStatus(event, SeatStatus.HELD);
        BigDecimal pricePerTicket = pricingService.calculateCurrentPrice(event, event.getAvailableTickets() + heldSeats);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("eventId", event.getId());
        response.put("seatNumbers", seats.stream().map(Seat::getSeatNumber).toList());
        response.put("pricePerTicket", pricePerTicket);
        response.put("totalAmount", pricePerTicket.multiply(BigDecimal.valueOf(seats.size())));
        response.put("holdExpiresAt", heldAt.plusMinutes(HOLD_MINUTES));
        return response;
    }

    @Transactional
    public void cancelHold(String email, Long eventId, List<Long> seatIds) {
        User user = getUser(email);
        Event event = eventService.getAccessibleEventEntity(eventId, email);
        List<Seat> seats = seatRepository.findByEventAndIdIn(event, seatIds);
        if (seats.size() != seatIds.size()) {
            throw new IllegalArgumentException("One or more selected seats do not exist");
        }

        seats.stream()
                .filter(seat -> seat.getStatus() == SeatStatus.HELD)
                .filter(seat -> seat.getHeldBy() != null && Objects.equals(seat.getHeldBy().getId(), user.getId()))
                .forEach(seat -> {
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seat.setHeldBy(null);
                    seat.setHeldAt(null);
                });
        seatRepository.saveAll(seats);
        eventService.refreshAvailableTickets(event);
    }

    @Transactional
    public BookingResponse cancelBooking(String email, Long bookingId) {
        User user = getUser(email);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Booking does not belong to the current user");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Booking is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        List<Seat> seats = seatRepository.findByEventOrderBySeatNumberAsc(booking.getEvent()).stream()
                .sorted(SeatNumberComparator.BY_SEAT_NUMBER)
                .filter(seat -> seat.getBooking() != null && seat.getBooking().getId().equals(booking.getId()))
                .toList();
        seats.forEach(seat -> {
            seat.setBooking(null);
            seat.setHeldBy(null);
            seat.setHeldAt(null);
            seat.setStatus(SeatStatus.AVAILABLE);
        });
        seatRepository.saveAll(seats);
        eventService.refreshAvailableTickets(booking.getEvent());

        notificationService.createNotification(user, NotificationType.TICKET_CANCELLATION,
                "Your booking for " + booking.getEvent().getName() + " has been cancelled.");
        emailService.sendCancellationEmail(user, booking,
                "If a payment was collected, the eligible refund will be processed as per college policy.");
        return toResponse(booking);
    }

    @Transactional
    public void releaseExpiredHolds(Event event) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(HOLD_MINUTES);
        List<Seat> expiredSeats = seatRepository.findByStatusAndHeldAtBefore(SeatStatus.HELD, threshold).stream()
                .filter(seat -> seat.getEvent().getId().equals(event.getId()))
                .toList();
        if (expiredSeats.isEmpty()) {
            return;
        }
        expiredSeats.forEach(seat -> {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setHeldBy(null);
            seat.setHeldAt(null);
        });
        seatRepository.saveAll(expiredSeats);
        eventService.refreshAvailableTickets(event);
    }

    private User getUser(String email) {
        User user = getAccount(email);
        if (user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Admin cannot book tickets");
        }
        return user;
    }

    private User getAccount(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private int resolveTicketCount(BookingRequest request) {
        if (request.getSeatIds() != null && !request.getSeatIds().isEmpty()) {
            return request.getSeatIds().size();
        }
        return request.getTickets() == null ? 0 : request.getTickets();
    }

    private List<Seat> resolveSeatsForBooking(Event event, List<Long> requestedSeatIds, int ticketCount, User user) {
        if (requestedSeatIds != null && !requestedSeatIds.isEmpty()) {
            List<Seat> chosenSeats = seatRepository.findByEventAndIdIn(event, requestedSeatIds).stream()
                    .sorted(SeatNumberComparator.BY_SEAT_NUMBER)
                    .toList();
            for (Seat seat : chosenSeats) {
                boolean availableNow = seat.getStatus() == SeatStatus.AVAILABLE;
                boolean heldByCurrentUser = seat.getStatus() == SeatStatus.HELD
                        && seat.getHeldBy() != null
                        && Objects.equals(seat.getHeldBy().getId(), user.getId());
                if (!availableNow && !heldByCurrentUser) {
                    throw new IllegalArgumentException("Some selected seats are no longer available");
                }
            }
            return chosenSeats;
        }

        return seatRepository.findByEventAndStatusOrderBySeatNumberAsc(event, SeatStatus.AVAILABLE).stream()
                .sorted(SeatNumberComparator.BY_SEAT_NUMBER)
                .limit(ticketCount)
                .toList();
    }

    private BookingResponse finalizeBooking(Event event, User user, List<Seat> seats, List<TicketHolderDetails> ticketHolders,
                                            NotificationType notificationType, String notificationMessage) {
        int heldSeats = (int) seatRepository.countByEventAndStatus(event, SeatStatus.HELD);
        BigDecimal pricePerTicket = pricingService.calculateCurrentPrice(event, event.getAvailableTickets() + heldSeats);
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setEvent(event);
        booking.setTickets(seats.size());
        booking.setPricePerTicket(pricePerTicket);
        booking.setTotalAmount(pricePerTicket.multiply(BigDecimal.valueOf(seats.size())));
        booking.setBookingDate(LocalDateTime.now());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setSeatNumbers(String.join(", ", seats.stream()
                .sorted(SeatNumberComparator.BY_SEAT_NUMBER)
                .map(Seat::getSeatNumber)
                .toList()));
        booking.setTicketHoldersJson(writeTicketHolders(ticketHolders));
        booking = bookingRepository.save(booking);

        Booking persistedBooking = booking;
        seats.forEach(seat -> {
            seat.setStatus(SeatStatus.BOOKED);
            seat.setHeldBy(null);
            seat.setHeldAt(null);
            seat.setBooking(persistedBooking);
        });
        seatRepository.saveAll(seats);
        eventService.refreshAvailableTickets(event);

        notificationService.createNotification(user, notificationType, notificationMessage);
        emailService.sendBookingConfirmation(user, booking);
        return toResponse(booking);
    }

    private BookingResponse toResponse(Booking booking) {
        List<String> seatNumbers = new ArrayList<>();
        if (booking.getSeatNumbers() != null && !booking.getSeatNumbers().isBlank()) {
            for (String seatNumber : booking.getSeatNumbers().split(", ")) {
                seatNumbers.add(seatNumber);
            }
        }
        List<TicketHolderDetails> ticketHolders = readTicketHolders(booking.getTicketHoldersJson());
        return new BookingResponse(
                booking.getId(),
                booking.getEvent().getId(),
                booking.getEvent().getName(),
                booking.getTickets(),
                booking.getPricePerTicket(),
                booking.getTotalAmount(),
                booking.getBookingDate(),
                booking.getStatus().name(),
                seatNumbers,
                ticketHolders);
    }

    private List<TicketHolderDetails> normalizeTicketHolders(List<TicketHolderDetails> ticketHolders, int ticketCount) {
        if (ticketHolders == null || ticketHolders.isEmpty()) {
            throw new IllegalArgumentException("Ticket holder details are required");
        }
        if (ticketHolders.size() != ticketCount) {
            throw new IllegalArgumentException("Enter name, age, and gender for each ticket holder");
        }
        return ticketHolders.stream()
                .map(holder -> new TicketHolderDetails(
                        holder.getName() == null ? null : holder.getName().trim(),
                        holder.getAge(),
                        holder.getGender() == null ? null : holder.getGender().trim()))
                .toList();
    }

    private String buildBookingNotificationMessage(String eventName, List<TicketHolderDetails> ticketHolders) {
        return "Booking confirmed for " + eventName + ".\nTicket holders: " + formatTicketHolders(ticketHolders);
    }

    private String formatTicketHolders(List<TicketHolderDetails> ticketHolders) {
        return ticketHolders.stream()
                .map(holder -> holder.getName() + " (" + holder.getAge() + ", " + holder.getGender() + ")")
                .reduce((left, right) -> left + "; " + right)
                .orElse("Not provided");
    }

    private String writeTicketHolders(List<TicketHolderDetails> ticketHolders) {
        try {
            return objectMapper.writeValueAsString(ticketHolders);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to save ticket holder details", exception);
        }
    }

    private List<TicketHolderDetails> readTicketHolders(String ticketHoldersJson) {
        if (ticketHoldersJson == null || ticketHoldersJson.isBlank()) {
            return List.of();
        }
        String value = ticketHoldersJson.trim();
        if (!value.startsWith("[")) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<List<TicketHolderDetails>>() { });
        } catch (Exception exception) {
            return List.of();
        }
    }
}
