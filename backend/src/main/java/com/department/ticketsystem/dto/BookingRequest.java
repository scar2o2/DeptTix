package com.department.ticketsystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.util.List;

public class BookingRequest {

    @NotNull
    private Long eventId;

    @Min(1)
    private Integer tickets;

    private List<Long> seatIds;

    @NotNull(message = "Ticket holder details are required")
    @Size(min = 1, max = 8, message = "You can book between 1 and 8 tickets for an event")
    @Valid
    private List<TicketHolderDetails> ticketHolders;

    public BookingRequest() {
    }

    public BookingRequest(Long eventId, Integer tickets, List<Long> seatIds, List<TicketHolderDetails> ticketHolders) {
        this.eventId = eventId;
        this.tickets = tickets;
        this.seatIds = seatIds;
        this.ticketHolders = ticketHolders;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Integer getTickets() {
        return tickets;
    }

    public void setTickets(Integer tickets) {
        this.tickets = tickets;
    }

    public List<Long> getSeatIds() {
        return seatIds;
    }

    public void setSeatIds(List<Long> seatIds) {
        this.seatIds = seatIds;
    }

    public List<TicketHolderDetails> getTicketHolders() {
        return ticketHolders;
    }

    public void setTicketHolders(List<TicketHolderDetails> ticketHolders) {
        this.ticketHolders = ticketHolders;
    }
}
