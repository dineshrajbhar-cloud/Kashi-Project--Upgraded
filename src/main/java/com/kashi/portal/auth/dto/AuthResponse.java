package com.kashi.portal.auth.dto;

import com.kashi.portal.user.dto.UserResponse;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        UserResponse user) {
}
