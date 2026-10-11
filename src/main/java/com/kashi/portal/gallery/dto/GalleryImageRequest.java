package com.kashi.portal.gallery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record GalleryImageRequest(

        @Schema(example = "Evening Ganga Aarti")
        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must be at most 150 characters")
        String title,

        @Schema(example = "https://example.com/images/ganga-aarti.jpg")
        @NotBlank(message = "Image URL is required")
        @URL(regexp = "^https?://.*", message = "Image URL must be a valid http or https URL")
        @Size(max = 500, message = "Image URL must be at most 500 characters")
        String imageUrl,

        @Size(max = 500, message = "Caption must be at most 500 characters")
        String caption) {

    public GalleryImageRequest {
        if (title != null) {
            title = title.trim();
        }
        if (imageUrl != null) {
            imageUrl = imageUrl.trim();
        }
        if (caption != null) {
            caption = caption.isBlank() ? null : caption.trim();
        }
    }
}
