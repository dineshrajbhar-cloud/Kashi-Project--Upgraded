package com.kashi.portal.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

public record LoginRequest(

        @Schema(example = "asha@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        String email,

        @Schema(example = "StrongPass123")
        @NotBlank(message = "Password is required")
        String password) {

    public LoginRequest {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }
}
