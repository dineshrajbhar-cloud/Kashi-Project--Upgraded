package com.kashi.portal.gallery;

import com.kashi.portal.common.PageResponse;
import com.kashi.portal.gallery.dto.GalleryImageRequest;
import com.kashi.portal.gallery.dto.GalleryImageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/gallery")
@RequiredArgsConstructor
@Tag(name = "Gallery")
public class GalleryController {

    private final GalleryService galleryService;

    @GetMapping
    @Operation(summary = "List gallery images", description = "Public. The page size is capped at 50.")
    public PageResponse<GalleryImageResponse> getImages(
            @Parameter(description = "Page number, starting at 0")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Items per page (1 to 50)")
            @RequestParam(name = "size", defaultValue = "12") int size,
            @Parameter(description = "Field and direction. Fields: title, createdAt", example = "createdAt,desc")
            @RequestParam(name = "sort", defaultValue = "createdAt,desc") String sort) {
        return galleryService.getImages(page, size, sort);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add a gallery image URL (ADMIN)")
    public GalleryImageResponse addImage(@Valid @RequestBody GalleryImageRequest request) {
        return galleryService.addImage(request);
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update a gallery image (ADMIN)")
    public GalleryImageResponse updateImage(@PathVariable("id") Long id,
                                            @Valid @RequestBody GalleryImageRequest request) {
        return galleryService.updateImage(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a gallery image (ADMIN)")
    public void deleteImage(@PathVariable("id") Long id) {
        galleryService.deleteImage(id);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Upload a gallery image (ADMIN)")
    public GalleryImageResponse uploadImage(
            @RequestParam("title") String title,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestPart("file") MultipartFile file) {

        return galleryService.uploadImage(title, caption, file);
    }
}
