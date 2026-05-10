package com.department.ticketsystem.util;

import com.department.ticketsystem.model.Booking;
import com.department.ticketsystem.model.Event;
import com.department.ticketsystem.model.User;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class EmailTemplateBuilder {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    public String bookingConfirmation(User user, Booking booking) {
        Event event = booking.getEvent();
        return layout("Booking Confirmed",
                "Your tickets are confirmed, Veltech Events.",
                details(
                        row("Booking ID", "#" + booking.getId()),
                        row("Event", event.getName()),
                        row("Date & Time", formatDateTime(event)),
                        row("Venue", event.getVenue()),
                        row("Tickets", String.valueOf(booking.getTickets())),
                        row("Seats", emptyFallback(booking.getSeatNumbers(), "Seats assigned at venue")),
                        row("Total Amount", money(booking.getTotalAmount()))));
    }

    public String cancellation(User user, Booking booking, String refundInfo) {
        return layout("Booking Cancelled",
                "Your cancellation has been processed, " + safe(user.getName()) + ".",
                details(
                        row("Booking ID", "#" + booking.getId()),
                        row("Event", booking.getEvent().getName()),
                        row("Tickets Cancelled", String.valueOf(booking.getTickets())),
                        row("Refund Info", emptyFallback(refundInfo, "Refund details are not applicable."))));
    }

    public String eventUpdate(User user, Event event, String updateSummary) {
        return layout("Event Updated",
                "An event you booked has been updated, " + safe(user.getName()) + ".",
                details(
                        row("Event", event.getName()),
                        row("Updated Details", updateSummary),
                        row("Date & Time", formatDateTime(event)),
                        row("Venue", event.getVenue()),
                        row("Current Price", money(event.getTicketPrice()))));
    }

    public String waitlistPromotion(User user, Event event) {
        return layout("Waitlist Promotion",
                "Good news, " + safe(user.getName()) + ". Your waitlisted ticket is now confirmed.",
                details(
                        row("Event", event.getName()),
                        row("Date & Time", formatDateTime(event)),
                        row("Venue", event.getVenue()),
                        row("Ticket Price", money(event.getTicketPrice()))));
    }

    public String notification(User user, String heading, String message) {
        return layout(heading,
                "Hello " + safe(user.getName()) + ",",
                "<p style=\"margin:0;color:#334155;line-height:1.6;\">" + escape(message) + "</p>");
    }

    private String layout(String title, String intro, String content) {
        return """
                <!doctype html>
                <html>
                <body style="margin:0;padding:0;background:#f4f7fb;font-family:Arial,Helvetica,sans-serif;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f4f7fb;padding:28px 0;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="640" cellspacing="0" cellpadding="0" style="max-width:640px;width:94%%;background:#ffffff;border:1px solid #dbe3ef;border-radius:8px;overflow:hidden;">
                          <tr>
                            <td style="background:#0f172a;color:#ffffff;padding:22px 28px;">
                              <div style="font-size:13px;letter-spacing:.08em;text-transform:uppercase;color:#cbd5e1;">College Event Ticketing</div>
                              <h1 style="margin:10px 0 0;font-size:24px;line-height:1.25;">%s</h1>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:28px;">
                              <p style="margin:0 0 20px;color:#334155;font-size:16px;line-height:1.6;">%s</p>
                              %s
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(escape(title), escape(intro), content);
    }

    private String details(String... rows) {
        return """
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="border-collapse:collapse;border:1px solid #e2e8f0;border-radius:8px;overflow:hidden;">
                  %s
                </table>
                """.formatted(String.join("", rows));
    }

    private String row(String label, String value) {
        return """
                <tr>
                  <td style="padding:12px 14px;background:#f8fafc;border-bottom:1px solid #e2e8f0;color:#475569;font-weight:bold;width:38%%;">%s</td>
                  <td style="padding:12px 14px;border-bottom:1px solid #e2e8f0;color:#0f172a;">%s</td>
                </tr>
                """.formatted(escape(label), escape(value));
    }

    private String formatDateTime(Event event) {
        return event.getDateTime() == null ? "To be announced" : event.getDateTime().format(DATE_TIME_FORMATTER);
    }

    private String money(BigDecimal amount) {
        return amount == null ? "Not available" : "Rs. " + amount;
    }

    private String emptyFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "there" : value;
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
