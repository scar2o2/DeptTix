package com.department.ticketsystem.repository;

import com.department.ticketsystem.model.Event;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findAllByOrderByDateTimeAsc();

    List<Event> findByDateTimeBetween(LocalDateTime start, LocalDateTime end);
}
