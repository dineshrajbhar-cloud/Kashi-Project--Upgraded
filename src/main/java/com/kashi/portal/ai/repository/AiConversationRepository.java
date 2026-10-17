package com.kashi.portal.ai.repository;

import com.kashi.portal.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

    Optional<AiConversation> findByConversationId(String conversationId);

    boolean existsByConversationId(String conversationId);

}
