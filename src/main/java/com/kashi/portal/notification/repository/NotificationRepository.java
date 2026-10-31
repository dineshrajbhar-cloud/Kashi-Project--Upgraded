package com.kashi.portal.notification.repository;

import com.kashi.portal.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Get all notifications of a user (latest first)
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Get unread notifications
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    // Count unread notifications
    long countByUserIdAndReadFalse(Long userId);

    // Find notification belonging to a specific user
    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    // Delete all notifications of a user
    void deleteByUserId(Long userId);
}