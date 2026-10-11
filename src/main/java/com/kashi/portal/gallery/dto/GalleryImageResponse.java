package com.kashi.portal.gallery.dto;

import com.kashi.portal.gallery.GalleryImage;

import java.time.LocalDateTime;

public record GalleryImageResponse(
        Long id,
        String title,
        String imageUrl,
        String caption,
        LocalDateTime createdAt) {

    public static GalleryImageResponse from(GalleryImage image) {
        return new GalleryImageResponse(
                image.getId(),
                image.getTitle(),
                image.getImageUrl(),
                image.getCaption(),
                image.getCreatedAt());
    }
}
