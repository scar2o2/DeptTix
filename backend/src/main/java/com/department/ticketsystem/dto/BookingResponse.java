package com.department.ticketsystem.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class BookingResponse {

    private Long id;
    private Long eventId;
    private String eventName;
    private Integer tickets;
    private BigDecimal pricePerTicket;
    private BigDecimal totalAmount;
    private LocalDateTime bookingDate;
    private String status;
    private List<String> seatNumbers;
    private List<TicketHolderDetails> ticketHolders;

    public BookingResponse() {
    }

    public BookingResponse(Long id, Long eventId, String eventName, Integer tickets, BigDecimal pricePerTicket,
                           BigDecimal totalAmount, LocalDateTime bookingDate, String status, List<String> seatNumbers,
                           List<TicketHolderDetails> ticketHolders) {
        this.id = id;
        this.eventId = eventId;
        this.eventName = eventName;
        this.tickets = tickets;
        this.pricePerTicket = pricePerTicket;
        this.totalAmount = totalAmount;
        this.bookingDate = bookingDate;
        this.status = status;
        this.seatNumbers = seatNumbers;
        this.ticketHolders = ticketHolders;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public Integer getTickets() {
        return tickets;
    }

    public void setTickets(Integer tickets) {
        this.tickets = tickets;
    }

    public BigDecimal getPricePerTicket() {
        return pricePerTicket;
    }

    public void setPricePerTicket(BigDecimal pricePerTicket) {
        this.pricePerTicket = pricePerTicket;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDateTime bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getSeatNumbers() {
        return seatNumbers;
    }

    public void setSeatNumbers(List<String> seatNumbers) {
        this.seatNumbers = seatNumbers;
    }

    public List<TicketHolderDetails> getTicketHolders() {
        return ticketHolders;
    }

    public void setTicketHolders(List<TicketHolderDetails> ticketHolders) {
        this.ticketHolders = ticketHolders;
    }
}
