package com.kashi.portal.place.dto;

import com.kashi.portal.place.Place;
import com.kashi.portal.place.PlaceType;
import com.kashi.portal.place.BookingType;
import java.math.BigDecimal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PlaceResponse(
        Long id,
        String name,
        PlaceType type,
        String shortDescription,
        String description,
        String imageUrl,
        String sourceUrl,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean bookingAvailable,
        BookingType bookingType,
        BigDecimal pricePerPerson,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static PlaceResponse from(Place place) {
        return new PlaceResponse(
                place.getId(),
                place.getName(),
                place.getType(),
                place.getShortDescription(),
                place.getDescription(),
                place.getImageUrl(),
                place.getSourceUrl(),
                place.getLatitude(),
                place.getLongitude(),
                place.isBookingAvailable(),
                place.getBookingType(),
                place.getPricePerPerson(),
                place.getCreatedAt(),
                place.getUpdatedAt());
    }
}
