CREATE TABLE ai_conversations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id VARCHAR(36) NOT NULL,
    title VARCHAR(150) NOT NULL DEFAULT 'New Conversation',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    CONSTRAINT uk_ai_conversations_conversation_id
        UNIQUE (conversation_id)
);

--CREATE TABLE ai_chat_messages (
--    id BIGINT NOT NULL AUTO_INCREMENT,
--    conversation_id BIGINT NOT NULL,
--    role VARCHAR(20) NOT NULL,
--    content TEXT NOT NULL,
--    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--
--    PRIMARY KEY (id),
--
--    CONSTRAINT fk_ai_messages_conversation
--        FOREIGN KEY (conversation_id)
--        REFERENCES ai_conversations(id)
--        ON DELETE CASCADE,
--
--    CONSTRAINT chk_ai_message_role
--        CHECK (role IN ('user', 'assistant'))
--);
--
--CREATE INDEX idx_ai_messages_conversation_created
--    ON ai_chat_messages (conversation_id, created_at, id);