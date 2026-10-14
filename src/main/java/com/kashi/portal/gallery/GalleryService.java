package com.kashi.portal.gallery;

import com.kashi.portal.common.PageResponse;
import com.kashi.portal.common.Paging;
import com.kashi.portal.common.ResourceNotFoundException;
import com.kashi.portal.gallery.dto.GalleryImageRequest;
import com.kashi.portal.gallery.dto.GalleryImageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GalleryService {

    private static final Set<String> SORT_FIELDS = Set.of("title", "createdAt");

    private final GalleryImageRepository galleryImageRepository;
    private final ImageStorageService imageStorageService;

    public PageResponse<GalleryImageResponse> getImages(int page, int size, String sort) {
        Pageable pageable = Paging.of(page, size, sort, SORT_FIELDS);
        return PageResponse.from(galleryImageRepository.findAll(pageable).map(GalleryImageResponse::from));
    }

    @Transactional
    public GalleryImageResponse addImage(GalleryImageRequest request) {
        GalleryImage image = new GalleryImage();
        copyFields(request, image);
        return GalleryImageResponse.from(galleryImageRepository.save(image));
    }

    @Transactional
    public GalleryImageResponse updateImage(Long id, GalleryImageRequest request) {
        GalleryImage image = findImage(id);
        copyFields(request, image);
        return GalleryImageResponse.from(galleryImageRepository.save(image));
    }

    @Transactional
    public void deleteImage(Long id) {
        galleryImageRepository.delete(findImage(id));
    }

    private GalleryImage findImage(Long id) {
        return galleryImageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gallery image not found with id " + id));
    }

    private void copyFields(GalleryImageRequest request, GalleryImage image) {
        image.setTitle(request.title());
        image.setImageUrl(request.imageUrl());
        image.setCaption(request.caption());
    }

    @Transactional
    public GalleryImageResponse uploadImage(
            String title,
            String caption,
            MultipartFile file) {

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required.");
        }

        if (title.trim().length() > 150) {
            throw new IllegalArgumentException(
                    "Title must be at most 150 characters."
            );
        }

        if (caption != null && caption.trim().length() > 500) {
            throw new IllegalArgumentException(
                    "Caption must be at most 500 characters."
            );
        }

        String imageUrl = imageStorageService.store(file);

        GalleryImage image = new GalleryImage();
        image.setTitle(title.trim());
        image.setImageUrl(imageUrl);
        image.setCaption(
                caption == null || caption.isBlank()
                        ? null
                        : caption.trim()
        );

        return GalleryImageResponse.from(
                galleryImageRepository.save(image)
        );
    }
}
