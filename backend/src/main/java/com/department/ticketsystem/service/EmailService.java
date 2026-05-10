package com.department.ticketsystem.service;

import com.department.ticketsystem.model.Booking;
import com.department.ticketsystem.model.Event;
import com.department.ticketsystem.model.User;
import com.department.ticketsystem.util.EmailTemplateBuilder;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final EmailTemplateBuilder templateBuilder;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender,
                        EmailTemplateBuilder templateBuilder,
                        @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSender = mailSender;
        this.templateBuilder = templateBuilder;
        this.fromAddress = fromAddress;
    }

    @Async("emailTaskExecutor")
    public void sendBookingConfirmation(User user, Booking booking) {
        sendHtml(user.getEmail(),
                "Booking confirmed: " + booking.getEvent().getName(),
                templateBuilder.bookingConfirmation(user, booking));
    }

    @Async("emailTaskExecutor")
    public void sendCancellationEmail(User user, Booking booking, String refundInfo) {
        sendHtml(user.getEmail(),
                "Booking cancelled: " + booking.getEvent().getName(),
                templateBuilder.cancellation(user, booking, refundInfo));
    }

    @Async("emailTaskExecutor")
    public void sendEventUpdateEmail(List<User> users, Event event, String updateSummary) {
        for (User user : users) {
            sendHtml(user.getEmail(),
                    "Important event update: " + event.getName(),
                    templateBuilder.eventUpdate(user, event, updateSummary));
        }
    }

    @Async("emailTaskExecutor")
    public void sendWaitlistPromotionEmail(User user, Event event) {
        sendHtml(user.getEmail(),
                "Waitlist confirmed: " + event.getName(),
                templateBuilder.waitlistPromotion(user, event));
    }

    @Async("emailTaskExecutor")
    public void sendNotificationEmail(User user, String subject, String heading, String message) {
        sendHtml(user.getEmail(), subject, templateBuilder.notification(user, heading, message));
    }

    @Async("emailTaskExecutor")
    public void sendNotificationEmail(List<User> users, String subject, String heading, String message) {
        for (User user : users) {
            sendHtml(user.getEmail(), subject, templateBuilder.notification(user, heading, message));
        }
    }

    private void sendHtml(String to, String subject, String htmlBody) {
        try {
            if (to == null || to.isBlank()) {
                LOGGER.warn("Skipping email with subject '{}' because recipient is blank", subject);
                return;
            }
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            if (fromAddress != null && !fromAddress.isBlank()) {
                helper.setFrom(fromAddress);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception exception) {
            LOGGER.error("Failed to send email '{}' to {}", subject, to, exception);
        }
    }
}
