CREATE TABLE places (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    name              VARCHAR(150)  NOT NULL,
    type              VARCHAR(20)   NOT NULL,
    short_description VARCHAR(500)  NOT NULL,
    description       TEXT          NULL,
    image_url         VARCHAR(500)  NULL,
    source_url        VARCHAR(500)  NULL,
    latitude          DECIMAL(10,7) NULL,
    longitude         DECIMAL(10,7) NULL,
    created_at        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT chk_places_type CHECK (type IN ('GHAT', 'TEMPLE')),
    INDEX idx_places_type (type),
    INDEX idx_places_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
