package com.kashi.portal.ai.repository;

import com.kashi.portal.entity.AiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {

    List<AiChatMessage> findByConversation_ConversationIdOrderByCreatedAtAscIdAsc(
            String conversationId
    );
}
