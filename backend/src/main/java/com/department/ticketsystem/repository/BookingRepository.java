package com.department.ticketsystem.repository;

import com.department.ticketsystem.model.Booking;
import com.department.ticketsystem.model.BookingStatus;
import com.department.ticketsystem.model.Event;
import com.department.ticketsystem.model.User;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserOrderByBookingDateDesc(User user);

    List<Booking> findByEventOrderByBookingDateDesc(Event event);

    List<Booking> findByEventAndStatusOrderByBookingDateDesc(Event event, BookingStatus status);

    boolean existsByEvent(Event event);

    void deleteByEvent(Event event);

    @Query("""
        select b.event.name, sum(b.tickets)
        from Booking b
        where b.status = com.department.ticketsystem.model.BookingStatus.CONFIRMED
        group by b.event.id, b.event.name
        order by sum(b.tickets) desc
        """)
    List<Object[]> getBookingTotalsByEvent();

    @Query("""
        select function('date', b.bookingDate), sum(b.tickets)
        from Booking b
        where b.bookingDate >= :startDate
        and b.status = com.department.ticketsystem.model.BookingStatus.CONFIRMED
        group by function('date', b.bookingDate)
        order by function('date', b.bookingDate)
        """)
    List<Object[]> getBookingsOverTime(@Param("startDate") LocalDateTime startDate);

    @Query("""
        select b.event.id, b.event.name, coalesce(sum(b.totalAmount), 0), count(b), coalesce(sum(b.tickets), 0)
        from Booking b
        where b.status = com.department.ticketsystem.model.BookingStatus.CONFIRMED
        group by b.event.id, b.event.name
        order by coalesce(sum(b.totalAmount), 0) desc
        """)
    List<Object[]> getRevenueByEvent();
}
