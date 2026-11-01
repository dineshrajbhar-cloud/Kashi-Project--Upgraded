CREATE TABLE bookings (
    id BIGINT NOT NULL AUTO_INCREMENT,

    user_id BIGINT NOT NULL,
    place_id BIGINT NOT NULL,

    booking_date DATE NOT NULL,
    visitors INT NOT NULL,

    booking_type VARCHAR(20) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,

    booking_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED',

    special_request TEXT,

    razorpay_order_id VARCHAR(100),
    razorpay_payment_id VARCHAR(100),

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    CONSTRAINT fk_booking_user
        FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT fk_booking_place
        FOREIGN KEY (place_id) REFERENCES places(id),

    CONSTRAINT chk_booking_visitors
        CHECK (visitors > 0),

    CONSTRAINT chk_booking_amount
        CHECK (total_amount >= 0),

    INDEX idx_booking_user (user_id),
    INDEX idx_booking_place (place_id),
    INDEX idx_booking_status (booking_status),
    INDEX idx_booking_date (booking_date)
);