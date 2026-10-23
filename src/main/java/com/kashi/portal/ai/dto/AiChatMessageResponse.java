package com.kashi.portal.ai.dto;

import java.time.LocalDateTime;

public record AiChatMessageResponse(
        String role,
        String content,
        LocalDateTime createdAt
) {
}
