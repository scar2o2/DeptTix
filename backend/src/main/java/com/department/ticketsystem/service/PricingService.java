package com.department.ticketsystem.service;

import com.department.ticketsystem.model.Event;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class PricingService {

    public BigDecimal calculateCurrentPrice(Event event, int sellableAvailableSeats) {
        if (event.getTotalTickets() == null || event.getTotalTickets() == 0) {
            return event.getTicketPrice();
        }

        int soldTickets = Math.max(0, event.getTotalTickets() - sellableAvailableSeats);
        double soldPercentage = (soldTickets * 100.0) / event.getTotalTickets();
        BigDecimal multiplier = BigDecimal.ONE;

        if (soldPercentage >= 80.0) {
            multiplier = BigDecimal.valueOf(1.5);
        } else if (soldPercentage >= 50.0) {
            multiplier = BigDecimal.valueOf(1.2);
        }

        long hoursUntilEvent = Duration.between(LocalDateTime.now(), event.getDateTime()).toHours();
        if (hoursUntilEvent >= 0 && hoursUntilEvent <= 48) {
            multiplier = multiplier.add(BigDecimal.valueOf(0.1));
        }

        return event.getTicketPrice().multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateMultiplier(Event event, int sellableAvailableSeats) {
        return calculateCurrentPrice(event, sellableAvailableSeats)
                .divide(event.getTicketPrice(), 2, RoundingMode.HALF_UP);
    }
}
