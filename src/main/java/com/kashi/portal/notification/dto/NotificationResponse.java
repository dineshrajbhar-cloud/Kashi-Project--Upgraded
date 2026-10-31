package com.kashi.portal.notification.dto;

import com.kashi.portal.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(

        Long id,

        String title,

        String message,

        NotificationType type,

        boolean read,

        String referenceType,

        Long referenceId,

        LocalDateTime createdAt

) {
}