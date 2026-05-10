package com.department.ticketsystem.service;

import com.department.ticketsystem.model.Booking;
import com.department.ticketsystem.model.BookingStatus;
import com.department.ticketsystem.model.Event;
import com.department.ticketsystem.model.NotificationType;
import com.department.ticketsystem.model.User;
import com.department.ticketsystem.repository.BookingRepository;
import com.department.ticketsystem.repository.EventRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class EventReminderService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    public EventReminderService(EventRepository eventRepository,
                                BookingRepository bookingRepository,
                                NotificationService notificationService,
                                EmailService emailService) {
        this.eventRepository = eventRepository;
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void sendOneDayEventReminders() {
        LocalDateTime start = LocalDateTime.now().plusHours(23);
        LocalDateTime end = LocalDateTime.now().plusHours(24);
        List<Event> upcomingEvents = eventRepository.findByDateTimeBetween(start, end);

        for (Event event : upcomingEvents) {
            String message = "Reminder: " + event.getName() + " starts on "
                    + event.getDateTime().format(DATE_TIME_FORMATTER)
                    + " at " + event.getVenue() + ".";
            List<User> attendees = bookingRepository
                    .findByEventAndStatusOrderByBookingDateDesc(event, BookingStatus.CONFIRMED)
                    .stream()
                    .map(Booking::getUser)
                    .filter(user -> user.getEmail() != null && !user.getEmail().isBlank())
                    .distinct()
                    .toList();

            for (User user : attendees) {
                notificationService.createNotification(user, NotificationType.EVENT_UPDATE, message);
            }
            emailService.sendNotificationEmail(attendees, "Event reminder: " + event.getName(), "Event Reminder", message);
        }
    }
}
