package com.department.ticketsystem.repository;

import com.department.ticketsystem.model.Notification;
import com.department.ticketsystem.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByTimestampDesc(User user);

    long countByUserAndReadStatusFalse(User user);
}
