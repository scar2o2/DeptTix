package com.department.ticketsystem.controller;

import com.department.ticketsystem.dto.NotificationResponse;
import com.department.ticketsystem.service.NotificationService;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> getNotifications(Principal principal) {
        return notificationService.getNotifications(principal.getName());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount(Principal principal) {
        return Map.of("count", notificationService.getUnreadCount(principal.getName()));
    }

    @PatchMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id, Principal principal) {
        notificationService.markAsRead(principal.getName(), id);
    }

    @PatchMapping("/read-all")
    public void markAllAsRead(Principal principal) {
        notificationService.markAllAsRead(principal.getName());
    }
}
