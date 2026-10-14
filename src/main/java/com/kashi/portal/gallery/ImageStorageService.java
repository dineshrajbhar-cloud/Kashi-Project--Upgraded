package com.kashi.portal.gallery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import java.io.InputStream;

@Service
public class ImageStorageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final Path uploadDirectory;

    public ImageStorageService(
            @Value("${app.upload.dir:uploads/gallery}") String uploadDir) {

        this.uploadDirectory = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();
    }

    public String store(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please select an image."
            );
        }


        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Image size must not exceed 5 MB."
            );
        }

        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only JPEG, PNG and WebP images are allowed."
            );
        }

        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(12);

            boolean isJpeg =
                    header.length >= 3
                            && (header[0] & 0xFF) == 0xFF
                            && (header[1] & 0xFF) == 0xD8
                            && (header[2] & 0xFF) == 0xFF;

            boolean isPng =
                    header.length >= 8
                            && (header[0] & 0xFF) == 0x89
                            && header[1] == 0x50
                            && header[2] == 0x4E
                            && header[3] == 0x47
                            && header[4] == 0x0D
                            && header[5] == 0x0A
                            && header[6] == 0x1A
                            && header[7] == 0x0A;

            boolean isWebp =
                    header.length >= 12
                            && header[0] == 'R'
                            && header[1] == 'I'
                            && header[2] == 'F'
                            && header[3] == 'F'
                            && header[8] == 'W'
                            && header[9] == 'E'
                            && header[10] == 'B'
                            && header[11] == 'P';

            boolean validSignature = switch (contentType) {
                case "image/jpeg" -> isJpeg;
                case "image/png" -> isPng;
                case "image/webp" -> isWebp;
                default -> false;
            };

            if (!validSignature) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "The file content does not match the selected image format."
                );
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String extension = switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported image format."
            );
        };

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() == null
                        ? ""
                        : file.getOriginalFilename()
        );

        if (originalFilename.isBlank()
                || originalFilename.contains("..")
                || originalFilename.contains("/")
                || originalFilename.contains("\\")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid filename."
            );
        }

        String filename = UUID.randomUUID() + extension;
        Path destination = uploadDirectory.resolve(filename).normalize();

        if (!destination.startsWith(uploadDirectory)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid upload path."
            );
        }

        try {
            Files.createDirectories(uploadDirectory);
            Files.copy(file.getInputStream(), destination);

            return "/uploads/gallery/" + filename;

        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not save the uploaded image."
            );
        }
    }
}
