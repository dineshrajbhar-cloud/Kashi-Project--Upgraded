package com.kashi.portal.feedback.dto;

import com.kashi.portal.feedback.Feedback;

import java.time.LocalDateTime;

public record FeedbackResponse(
        Long id,
        String name,
        String email,
        String message,
        Long userId,
        LocalDateTime createdAt) {

    public static FeedbackResponse from(Feedback feedback) {
        Long userId = feedback.getUser() != null ? feedback.getUser().getId() : null;
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getName(),
                feedback.getEmail(),
                feedback.getMessage(),
                userId,
                feedback.getCreatedAt());
    }
}
