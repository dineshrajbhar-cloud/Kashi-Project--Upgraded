package com.kashi.portal.notification.service.impl;

import com.kashi.portal.common.ResourceNotFoundException;
import com.kashi.portal.notification.dto.NotificationResponse;
import com.kashi.portal.notification.entity.Notification;
import com.kashi.portal.notification.entity.NotificationType;
import com.kashi.portal.notification.repository.NotificationRepository;
import com.kashi.portal.notification.service.NotificationService;
import com.kashi.portal.user.User;
import com.kashi.portal.user.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // Create notification
    @Override
    @Transactional
    public void createNotification(
            Long userId,
            String title,
            String message,
            NotificationType type,
            String referenceType,
            Long referenceId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found.")
                );

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .read(false)
                .build();

        notificationRepository.save(notification);
    }

    // Get all notifications
    @Override
    public List<NotificationResponse> getMyNotifications(Long userId) {

        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Get unread notifications
    @Override
    public List<NotificationResponse> getUnreadNotifications(Long userId) {

        return notificationRepository
                .findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Get unread notification count
    @Override
    public long getUnreadCount(Long userId) {

        return notificationRepository
                .countByUserIdAndReadFalse(userId);
    }

    // Mark single notification as read
    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {

        Notification notification = notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Notification not found.")
                );

        notification.setRead(true);

        notificationRepository.save(notification);
    }

    // Mark all notifications as read
    @Override
    @Transactional
    public void markAllAsRead(Long userId) {

        List<Notification> notifications = notificationRepository
                .findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);

        notifications.forEach(notification ->
                notification.setRead(true)
        );

        notificationRepository.saveAll(notifications);
    }

    // Delete single notification
    @Override
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {

        Notification notification = notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Notification not found.")
                );

        notificationRepository.delete(notification);
    }

    // Delete all notifications
    @Override
    @Transactional
    public void deleteAllNotifications(Long userId) {

        notificationRepository.deleteByUserId(userId);
    }

    // Entity to DTO mapping
    private NotificationResponse mapToResponse(Notification notification) {

        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.getCreatedAt()
        );
    }
}