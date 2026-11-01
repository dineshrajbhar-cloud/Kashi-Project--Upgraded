package com.kashi.portal.booking.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record BookingRequest(

        @NotNull(message = "Place ID is required")
        Long placeId,

        @NotNull(message = "Booking date is required")
        @FutureOrPresent(message = "Booking date cannot be in the past")
        LocalDate bookingDate,

        @NotNull(message = "Number of visitors is required")
        @Min(value = 1, message = "At least one visitor is required")
        Integer visitors,

        @Size(max = 1000, message = "Special request must be at most 1000 characters")
        String specialRequest

) {
    public BookingRequest {
        if (specialRequest != null && specialRequest.isBlank()) {
            specialRequest = null;
        } else if (specialRequest != null) {
            specialRequest = specialRequest.trim();
        }
    }
}