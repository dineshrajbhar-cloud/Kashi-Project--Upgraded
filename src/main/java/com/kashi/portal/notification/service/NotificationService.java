package com.kashi.portal.notification.service;

import com.kashi.portal.notification.dto.NotificationResponse;
import com.kashi.portal.notification.entity.NotificationType;

import java.util.List;

public interface NotificationService {

    // Create notification
    void createNotification(
            Long userId,
            String title,
            String message,
            NotificationType type,
            String referenceType,
            Long referenceId
    );

    // Get all notifications
    List<NotificationResponse> getMyNotifications(Long userId);

    // Get unread notifications
    List<NotificationResponse> getUnreadNotifications(Long userId);

    // Get unread notification count
    long getUnreadCount(Long userId);

    // Mark single notification as read
    void markAsRead(Long notificationId, Long userId);

    // Mark all notifications as read
    void markAllAsRead(Long userId);

    // Delete single notification
    void deleteNotification(Long notificationId, Long userId);

    // Delete all notifications
    void deleteAllNotifications(Long userId);
}