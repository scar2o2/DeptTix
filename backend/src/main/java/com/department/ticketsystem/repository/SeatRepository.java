package com.department.ticketsystem.repository;

import com.department.ticketsystem.model.Event;
import com.department.ticketsystem.model.Seat;
import com.department.ticketsystem.model.SeatStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByEventOrderBySeatNumberAsc(Event event);

    List<Seat> findByEventAndStatusOrderBySeatNumberAsc(Event event, SeatStatus status);

    List<Seat> findByEventAndIdIn(Event event, List<Long> seatIds);

    long countByEventAndStatus(Event event, SeatStatus status);

    @Query("""
        select s.event.id, s.status, count(s)
        from Seat s
        group by s.event.id, s.status
        """)
    List<Object[]> countSeatsByEventAndStatus();

    Optional<Seat> findByEventAndSeatNumber(Event event, String seatNumber);

    List<Seat> findByStatusAndHeldAtBefore(SeatStatus status, LocalDateTime threshold);

    void deleteByEvent(Event event);
}
