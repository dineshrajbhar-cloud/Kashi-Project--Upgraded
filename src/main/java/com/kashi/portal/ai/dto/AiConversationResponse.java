package com.kashi.portal.ai.dto;

import java.time.LocalDateTime;

public record AiConversationResponse(
        String conversationId,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
