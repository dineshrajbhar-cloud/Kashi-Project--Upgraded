CREATE TABLE revoked_tokens (
    token_id VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (token_id),
    KEY idx_revoked_tokens_expires_at (expires_at)
);