package com.department.ticketsystem.config;

import com.department.ticketsystem.model.Department;
import com.department.ticketsystem.model.Event;
import com.department.ticketsystem.model.Role;
import com.department.ticketsystem.model.Seat;
import com.department.ticketsystem.model.SeatStatus;
import com.department.ticketsystem.model.User;
import com.department.ticketsystem.model.Venue;
import com.department.ticketsystem.repository.EventRepository;
import com.department.ticketsystem.repository.SeatRepository;
import com.department.ticketsystem.repository.UserRepository;
import com.department.ticketsystem.repository.VenueRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataInitializer.class);
    private static final String LEGACY_PASSWORD_PLACEHOLDER = "FIREBASE_AUTH_LEGACY_PLACEHOLDER";
    private static final String ADMIN_EMAIL = "manojcherukuri202@gmai.com";
    private static final String ADMIN_PASSWORD = "Admin@123";
    private static final String ADMIN_NAME = "Admin";
    private static final List<String> CAMPUS_VENUES = List.of(
            "CK Naidu Stadium",
            "Convocation Hall",
            "MG Auditorium",
            "Placement Training Centre",
            "Gallery Hall");

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final VenueRepository venueRepository;

    public DataInitializer(UserRepository userRepository, EventRepository eventRepository,
                           SeatRepository seatRepository, VenueRepository venueRepository) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.venueRepository = venueRepository;
    }

    @Override
    public void run(String... args) {
        ensureAdminUser();
        ensureVenues();
        ensureSampleEvents();
    }

    private void ensureVenues() {
        for (String venueName : CAMPUS_VENUES) {
            if (!venueRepository.existsByNameIgnoreCase(venueName)) {
                Venue venue = new Venue();
                venue.setName(venueName);
                venueRepository.save(venue);
            }
        }
    }

    private void ensureAdminUser() {
        User admin = userRepository.findByEmail(ADMIN_EMAIL).orElseGet(User::new);
        admin.setName(ADMIN_NAME);
        admin.setEmail(ADMIN_EMAIL);
        admin.setPassword(LEGACY_PASSWORD_PLACEHOLDER);
        admin.setRole(Role.ADMIN);
        admin.setDepartment(Department.CSE.name());
        try {
            UserRecord firebaseAdmin = getOrCreateFirebaseAdmin();
            admin.setFirebaseUid(firebaseAdmin.getUid());
        } catch (Exception exception) {
            LOGGER.warn("Unable to auto-create Firebase admin user for {}. "
                    + "The DB admin row will still be seeded, but Firebase login for this account "
                    + "must be created manually. Reason: {}", ADMIN_EMAIL, exception.getMessage());
        }
        userRepository.save(admin);
    }

    private UserRecord getOrCreateFirebaseAdmin() throws FirebaseAuthException {
        try {
            return FirebaseAuth.getInstance().getUserByEmail(ADMIN_EMAIL);
        } catch (FirebaseAuthException exception) {
            String errorCode = exception.getAuthErrorCode() == null
                    ? ""
                    : exception.getAuthErrorCode().name();
            if (!"USER_NOT_FOUND".equals(errorCode)) {
                throw exception;
            }
        }

        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(ADMIN_EMAIL)
                .setPassword(ADMIN_PASSWORD)
                .setDisplayName(ADMIN_NAME)
                .setEmailVerified(true)
                .setDisabled(false);
        return FirebaseAuth.getInstance().createUser(request);
    }

    private void ensureSampleEvents() {
        if (eventRepository.count() > 0) {
            return;
        }

        Event expo = new Event();
        expo.setName("Vel Tech Innovation & Research Expo");
        expo.setDepartment(Department.ALL.name());
        expo.setDateTime(LocalDateTime.now().plusDays(7));
        expo.setVenue("MG Auditorium");
        expo.setTicketPrice(BigDecimal.valueOf(150));
        expo.setAvailableTickets(120);
        expo.setTotalTickets(120);
        expo = eventRepository.save(expo);
        seedSeats(expo);

        Event workshop = new Event();
        workshop.setName("School of Computing AI Workshop");
        workshop.setDepartment(Department.CSE.name());
        workshop.setDateTime(LocalDateTime.now().plusDays(12));
        workshop.setVenue("Gallery Hall");
        workshop.setTicketPrice(BigDecimal.valueOf(250));
        workshop.setAvailableTickets(80);
        workshop.setTotalTickets(80);
        workshop = eventRepository.save(workshop);
        seedSeats(workshop);

        Event seminar = new Event();
        seminar.setName("Campus to Corporate Placement Talk");
        seminar.setDepartment(Department.ALL.name());
        seminar.setDateTime(LocalDateTime.now().plusDays(16));
        seminar.setVenue("Placement Training Centre");
        seminar.setTicketPrice(BigDecimal.valueOf(100));
        seminar.setAvailableTickets(150);
        seminar.setTotalTickets(150);
        seminar = eventRepository.save(seminar);
        seedSeats(seminar);
    }

    private void seedSeats(Event event) {
        List<Seat> seats = new ArrayList<>();
        for (int index = 1; index <= event.getTotalTickets(); index++) {
            Seat seat = new Seat();
            seat.setEvent(event);
            seat.setSeatNumber(String.valueOf((char) ('A' + ((index - 1) / 10))) + (((index - 1) % 10) + 1));
            seat.setStatus(SeatStatus.AVAILABLE);
            seats.add(seat);
        }
        seatRepository.saveAll(seats);
    }
}
