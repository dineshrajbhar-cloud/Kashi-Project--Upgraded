package com.kashi.portal.place.dto;

import com.kashi.portal.place.BookingType;
import com.kashi.portal.place.PlaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;

public record PlaceRequest(

        @Schema(example = "Assi Ghat")
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @Schema(description = "GHAT or TEMPLE", example = "GHAT")
        @NotNull(message = "Type is required (GHAT or TEMPLE)")
        PlaceType type,

        @Schema(example = "The southern ghat where the Assi stream meets the Ganga.")
        @NotBlank(message = "Short description is required")
        @Size(max = 500, message = "Short description must be at most 500 characters")
        String shortDescription,

        @Size(max = 10000, message = "Description must be at most 10000 characters")
        String description,

        @Schema(example = "https://example.com/images/assi-ghat.jpg")
        @URL(regexp = "^https?://.*", message = "Image URL must be a valid http or https URL")
        @Size(max = 500, message = "Image URL must be at most 500 characters")
        String imageUrl,

        @Schema(example = "https://en.wikipedia.org/wiki/Assi_Ghat")
        @URL(regexp = "^https?://.*", message = "Source URL must be a valid http or https URL")
        @Size(max = 500, message = "Source URL must be at most 500 characters")
        String sourceUrl,

        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        @Digits(integer = 3, fraction = 7, message = "Latitude can have at most 7 decimal places")
        BigDecimal latitude,

        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        @Digits(integer = 3, fraction = 7, message = "Longitude can have at most 7 decimal places")
        BigDecimal longitude,

        @Schema(example = "true")
        @NotNull(message = "Booking availability is required")
        Boolean bookingAvailable,

        @Schema(example = "PAID")
        @NotNull(message = "Booking type is required")
        BookingType bookingType,

        @Schema(example = "200.00")
        @NotNull(message = "Price per person is required")
        @DecimalMin(value = "0.00", message = "Price cannot be negative")
        @Digits(integer = 8, fraction = 2, message = "Price can have at most 2 decimal places")
        BigDecimal pricePerPerson

) {

    public PlaceRequest {
        name = trimToNull(name);
        shortDescription = trimToNull(shortDescription);
        description = trimToNull(description);
        imageUrl = trimToNull(imageUrl);
        sourceUrl = trimToNull(sourceUrl);
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}


//package com.kashi.portal.place.dto;
//
//import com.kashi.portal.place.PlaceType;
//import io.swagger.v3.oas.annotations.media.Schema;
//import jakarta.validation.constraints.DecimalMax;
//import jakarta.validation.constraints.DecimalMin;
//import jakarta.validation.constraints.Digits;
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Size;
//import org.hibernate.validator.constraints.URL;
//
//import java.math.BigDecimal;
//
//public record PlaceRequest(
//
//        @Schema(example = "Assi Ghat")
//        @NotBlank(message = "Name is required")
//        @Size(max = 150, message = "Name must be at most 150 characters")
//        String name,
//
//        @Schema(description = "GHAT or TEMPLE", example = "GHAT")
//        @NotNull(message = "Type is required (GHAT or TEMPLE)")
//        PlaceType type,
//
//        @Schema(example = "The southern ghat where the Assi stream meets the Ganga.")
//        @NotBlank(message = "Short description is required")
//        @Size(max = 500, message = "Short description must be at most 500 characters")
//        String shortDescription,
//
//        @Size(max = 10000, message = "Description must be at most 10000 characters")
//        String description,
//
//        @Schema(example = "https://example.com/images/assi-ghat.jpg")
//        @URL(regexp = "^https?://.*", message = "Image URL must be a valid http or https URL")
//        @Size(max = 500, message = "Image URL must be at most 500 characters")
//        String imageUrl,
//
//        @Schema(example = "https://en.wikipedia.org/wiki/Assi_Ghat")
//        @URL(regexp = "^https?://.*", message = "Source URL must be a valid http or https URL")
//        @Size(max = 500, message = "Source URL must be at most 500 characters")
//        String sourceUrl,
//
//        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
//        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
//        @Digits(integer = 3, fraction = 7, message = "Latitude can have at most 7 decimal places")
//        BigDecimal latitude,
//
//
//        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
//        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
//        @Digits(integer = 3, fraction = 7, message = "Longitude can have at most 7 decimal places")
//        BigDecimal longitude) {
//
//    public PlaceRequest {
//        name = trimToNull(name);
//        shortDescription = trimToNull(shortDescription);
//        description = trimToNull(description);
//        imageUrl = trimToNull(imageUrl);
//        sourceUrl = trimToNull(sourceUrl);
//    }
//
//    private static String trimToNull(String value) {
//        if (value == null || value.isBlank()) {
//            return null;
//        }
//        return value.trim();
//    }
//}
