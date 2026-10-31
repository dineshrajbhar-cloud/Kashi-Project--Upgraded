package com.kashi.portal.notification.controller;

import com.kashi.portal.notification.dto.NotificationResponse;
import com.kashi.portal.notification.service.NotificationService;
import com.kashi.portal.user.User;
import com.kashi.portal.user.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // Resolve authenticated user from JWT principal
    private User getAuthenticatedUser(Authentication authentication) {

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Authenticated user not found."
                        )
                );
    }

    // Get all notifications
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        return ResponseEntity.ok(
                notificationService.getMyNotifications(user.getId())
        );
    }

    // Get unread notifications
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        return ResponseEntity.ok(
                notificationService.getUnreadNotifications(user.getId())
        );
    }

    // Get unread notification count
    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadCount(
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        return ResponseEntity.ok(
                notificationService.getUnreadCount(user.getId())
        );
    }

    // Mark single notification as read
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        notificationService.markAsRead(id, user.getId());

        return ResponseEntity.noContent().build();
    }

    // Mark all notifications as read
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        notificationService.markAllAsRead(user.getId());

        return ResponseEntity.noContent().build();
    }

    // Delete single notification
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        notificationService.deleteNotification(id, user.getId());

        return ResponseEntity.noContent().build();
    }

    // Delete all notifications
    @DeleteMapping
    public ResponseEntity<Void> deleteAllNotifications(
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        notificationService.deleteAllNotifications(user.getId());

        return ResponseEntity.noContent().build();
    }
}
