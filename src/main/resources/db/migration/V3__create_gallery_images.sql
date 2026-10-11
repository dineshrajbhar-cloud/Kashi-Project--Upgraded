CREATE TABLE gallery_images (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    title      VARCHAR(150) NOT NULL,
    image_url  VARCHAR(500) NOT NULL,
    caption    VARCHAR(500) NULL,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_gallery_images_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
