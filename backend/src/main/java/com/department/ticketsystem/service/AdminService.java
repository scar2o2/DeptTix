package com.department.ticketsystem.service;

import com.department.ticketsystem.dto.BookingStatsResponse;
import com.department.ticketsystem.dto.RevenuePointResponse;
import com.department.ticketsystem.dto.TicketHolderDetails;
import com.department.ticketsystem.model.Booking;
import com.department.ticketsystem.model.BookingStatus;
import com.department.ticketsystem.model.Event;
import com.department.ticketsystem.repository.BookingRepository;
import com.department.ticketsystem.repository.EventRepository;
import com.department.ticketsystem.repository.SeatRepository;
import com.department.ticketsystem.model.SeatStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final ObjectMapper objectMapper;

    public AdminService(BookingRepository bookingRepository, EventRepository eventRepository,
                        SeatRepository seatRepository, ObjectMapper objectMapper) {
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.objectMapper = objectMapper;
    }

    public List<RevenuePointResponse> getRevenueData() {
        return bookingRepository.getRevenueByEvent().stream()
                .map(row -> new RevenuePointResponse(
                        ((Number) row[0]).longValue(),
                        row[1].toString(),
                        (BigDecimal) row[2],
                        ((Number) row[3]).longValue(),
                        ((Number) row[4]).longValue()))
                .toList();
    }

    public BookingStatsResponse getBookingStats() {
        List<Map<String, Object>> ticketDistribution = eventRepository.findAll().stream()
                .map(event -> Map.<String, Object>of(
                        "name", event.getName(),
                        "tickets", seatRepository.countByEventAndStatus(event, SeatStatus.BOOKED)))
                .toList();

        List<Map<String, Object>> soldVsRemaining = eventRepository.findAll().stream()
                .map(event -> Map.<String, Object>of(
                        "name", event.getName(),
                        "booked", seatRepository.countByEventAndStatus(event, SeatStatus.BOOKED),
                        "remaining", seatRepository.countByEventAndStatus(event, SeatStatus.AVAILABLE)))
                .toList();

        List<Map<String, Object>> bookingsOverTime = bookingRepository
                .getBookingsOverTime(LocalDateTime.now().minusDays(30)).stream()
                .map(row -> Map.<String, Object>of("date", row[0].toString(), "tickets", row[1]))
                .toList();

        return new BookingStatsResponse(ticketDistribution, bookingsOverTime, soldVsRemaining);
    }

    public byte[] exportReport(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        List<Booking> bookings = bookingRepository.findByEventAndStatusOrderByBookingDateDesc(event, BookingStatus.CONFIRMED);
        BigDecimal revenue = bookings.stream()
                .map(Booking::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();
            document.add(new Paragraph("Event Report"));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Event: " + event.getName()));
            document.add(new Paragraph("Department: " + event.getDepartment()));
            document.add(new Paragraph("Venue: " + event.getVenue()));
            document.add(new Paragraph("Date and Time: " + event.getDateTime()));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Total bookings: " + bookings.size()));
            document.add(new Paragraph("Tickets sold: " + bookings.stream().mapToInt(Booking::getTickets).sum()));
            document.add(new Paragraph("Revenue summary: Rs. " + revenue));
            document.add(new Paragraph(" "));
            for (Booking booking : bookings) {
                document.add(new Paragraph(
                        booking.getBookingDate() + " | " + booking.getUser().getName() + " | Seats: "
                                + booking.getSeatNumbers() + " | Total: Rs. " + booking.getTotalAmount()));
                document.add(new Paragraph("Ticket holders: " + formatTicketHolders(booking)));
                document.add(new Paragraph(" "));
            }
            document.close();
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate PDF report", exception);
        }
    }

    private String formatTicketHolders(Booking booking) {
        List<TicketHolderDetails> ticketHolders = readTicketHolders(booking.getTicketHoldersJson());
        if (ticketHolders.isEmpty()) {
            return "Not provided";
        }
        return ticketHolders.stream()
                .map(holder -> holder.getName() + " (" + holder.getAge() + ", " + holder.getGender() + ")")
                .reduce((left, right) -> left + "; " + right)
                .orElse("Not provided");
    }

    private List<TicketHolderDetails> readTicketHolders(String ticketHoldersJson) {
        if (ticketHoldersJson == null || ticketHoldersJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(ticketHoldersJson, new TypeReference<List<TicketHolderDetails>>() { });
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to read ticket holder details for export", exception);
        }
    }
}
