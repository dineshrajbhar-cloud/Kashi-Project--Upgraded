package com.kashi.portal.auth.dto;

import com.kashi.portal.user.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/** There is deliberately no role field: public registration always creates a USER. */
public record RegisterRequest(

        @Schema(example = "Asha Verma")
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @Schema(example = "asha@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 150, message = "Email must be at most 150 characters")
        String email,

        @Schema(example = "StrongPass123")
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password,

        @Schema(example = "Varanasi")
        @Size(max = 100, message = "City must be at most 100 characters")
        String city,

        @Schema(description = "MALE, FEMALE or OTHER", example = "FEMALE")
        Gender gender) {

    public RegisterRequest {
        if (name != null) {
            name = name.trim();
        }
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
        if (city != null) {
            city = city.trim().isEmpty() ? null : city.trim();
        }
    }
}
