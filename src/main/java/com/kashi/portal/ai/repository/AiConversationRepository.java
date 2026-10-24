package com.kashi.portal.ai.repository;

import com.kashi.portal.ai.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

    // Find a conversation only if it belongs to the specified user.
    Optional<AiConversation> findByConversationIdAndUser_Id( String conversationId, Long userId );

    // Check ownership before accessing or deleting a conversation.
    boolean existsByConversationIdAndUser_Id( String conversationId, Long userId );

    // Retrieve only the authenticated user's conversations.
    List<AiConversation> findAllByUser_IdOrderByUpdatedAtDesc(Long userId);

    boolean existsByConversationId(String conversationId);

//    Optional<AiConversation> findByConversationId(String conversationId);
//
//    boolean existsByConversationId(String conversationId);
//
//    List<AiConversation> findAllByOrderByUpdatedAtDesc();

}
