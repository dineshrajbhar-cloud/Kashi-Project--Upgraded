-- Remove old AI conversations.
-- Related messages are deleted through ON DELETE CASCADE.
DELETE FROM ai_conversations;

-- Add a required owner for every new conversation.
ALTER TABLE ai_conversations
    ADD COLUMN user_id BIGINT NOT NULL;

-- Link conversations to registered users.
ALTER TABLE ai_conversations
    ADD CONSTRAINT fk_ai_conversations_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE;

-- Index conversations by owner and update time.
CREATE INDEX idx_ai_conversations_user_updated
    ON ai_conversations(user_id, updated_at);




