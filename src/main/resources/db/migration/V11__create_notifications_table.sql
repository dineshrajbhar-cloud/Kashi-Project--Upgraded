CREATE TABLE notifications (

    id BIGINT NOT NULL AUTO_INCREMENT,

    user_id BIGINT NOT NULL,

    title VARCHAR(150) NOT NULL,

    message TEXT NOT NULL,

    type VARCHAR(30) NOT NULL,

    is_read BOOLEAN NOT NULL DEFAULT FALSE,

    reference_type VARCHAR(50),

    reference_id BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    INDEX idx_notifications_user_id (user_id),

    INDEX idx_notifications_user_read (user_id, is_read),

    INDEX idx_notifications_created_at (created_at)

);