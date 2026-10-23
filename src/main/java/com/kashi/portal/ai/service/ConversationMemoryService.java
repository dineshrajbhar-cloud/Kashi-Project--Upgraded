package com.kashi.portal.ai.service;

import com.kashi.portal.ai.entity.AiChatMessage;
import com.kashi.portal.ai.entity.AiConversation;
import com.kashi.portal.ai.repository.AiChatMessageRepository;
import com.kashi.portal.ai.repository.AiConversationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kashi.portal.ai.dto.AiConversationResponse;
import com.kashi.portal.ai.dto.AiChatMessageResponse;

import java.util.List;

@Service
public class ConversationMemoryService {

    private static final int MAX_HISTORY_MESSAGES = 10;

    private final AiConversationRepository conversationRepository;
    private final AiChatMessageRepository messageRepository;

    public ConversationMemoryService(
            AiConversationRepository conversationRepository,
            AiChatMessageRepository messageRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getHistory(String conversationId) {

        List<AiChatMessage> messages =
                messageRepository
                        .findByConversation_ConversationIdOrderByCreatedAtAscIdAsc(
                                conversationId
                        );

        int startIndex = Math.max(
                0,
                messages.size() - MAX_HISTORY_MESSAGES
        );

        return messages.subList(startIndex, messages.size())
                .stream()
                .map(message -> new ChatMessage(
                        message.getRole(),
                        message.getContent()
                ))
                .toList();
    }

    @Transactional
    public void addMessage(
            String conversationId,
            String role,
            String content
    ) {

        AiConversation conversation = conversationRepository
                .findByConversationId(conversationId)
                .orElseGet(() -> conversationRepository.save(
                        new AiConversation(
                                conversationId,
                                generateTitle(content)
                        )
                ));

        messageRepository.save(
                new AiChatMessage(conversation, role, content)
        );
    }

    @Transactional
    public void addExchange(
            String conversationId,
            String userMessage,
            String assistantReply
    ) {
        AiConversation conversation = conversationRepository
                .findByConversationId(conversationId)
                .orElseGet(() -> conversationRepository.save(
                        new AiConversation(
                                conversationId,
                                generateTitle(userMessage)
                        )
                ));

        messageRepository.saveAll(List.of(
                new AiChatMessage(conversation, "user", userMessage),
                new AiChatMessage(conversation, "assistant", assistantReply)
        ));
    }

    @Transactional(readOnly = true)
    public List<AiConversationResponse> getAllConversations() {
        return conversationRepository.findAllByOrderByUpdatedAtDesc()
                .stream()
                .map(conversation -> new AiConversationResponse(
                        conversation.getConversationId(),
                        conversation.getTitle(),
                        conversation.getCreatedAt(),
                        conversation.getUpdatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AiChatMessageResponse> getConversationMessages(
            String conversationId
    ) {
        return messageRepository
                .findByConversation_ConversationIdOrderByCreatedAtAscIdAsc(
                        conversationId
                )
                .stream()
                .map(message -> new AiChatMessageResponse(
                        message.getRole(),
                        message.getContent(),
                        message.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public boolean deleteConversation(String conversationId) {

        AiConversation conversation = conversationRepository
                .findByConversationId(conversationId)
                .orElse(null);

        if (conversation == null) {
            return false;
        }

        // Delete all messages associated with this conversation
        messageRepository.deleteByConversation_ConversationId(conversationId);

        // Delete the conversation
        conversationRepository.delete(conversation);

        return true;
    }

    private String generateTitle(String message) {
        if (message == null || message.isBlank()) {
            return "New Conversation";
        }

        String title = message.trim()
                .replaceAll("\\s+", " ");

        if (title.length() > 150) {
            title = title.substring(0, 147) + "...";
        }

        return title;
    }

    public record ChatMessage(String role, String content) {
    }
}
