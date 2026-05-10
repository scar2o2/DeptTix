package com.department.ticketsystem.service;

import com.department.ticketsystem.dto.NotificationResponse;
import com.department.ticketsystem.model.Notification;
import com.department.ticketsystem.model.NotificationType;
import com.department.ticketsystem.model.User;
import com.department.ticketsystem.repository.NotificationRepository;
import com.department.ticketsystem.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public void createNotification(User user, NotificationType type, String message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setMessage(message);
        notification.setReadStatus(false);
        notification.setTimestamp(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    public List<NotificationResponse> getNotifications(String email) {
        User user = getUser(email);
        return notificationRepository.findByUserOrderByTimestampDesc(user).stream()
                .map(this::toResponse)
                .toList();
    }

    public long getUnreadCount(String email) {
        return notificationRepository.countByUserAndReadStatusFalse(getUser(email));
    }

    @Transactional
    public void markAsRead(String email, Long id) {
        User user = getUser(email);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Notification does not belong to the current user");
        }
        notification.setReadStatus(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String email) {
        User user = getUser(email);
        List<Notification> notifications = notificationRepository.findByUserOrderByTimestampDesc(user);
        notifications.forEach(notification -> notification.setReadStatus(true));
        notificationRepository.saveAll(notifications);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getMessage(),
                notification.getType().name(),
                notification.isReadStatus(),
                notification.getTimestamp());
    }
}
