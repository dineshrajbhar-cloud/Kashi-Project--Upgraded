package com.kashi.portal.ai.controller;

import com.kashi.portal.ai.dto.AiChatMessageResponse;
import com.kashi.portal.ai.dto.AiChatRequest;
import com.kashi.portal.ai.dto.AiChatResponse;
import com.kashi.portal.ai.dto.AiConversationResponse;
import com.kashi.portal.ai.repository.AiConversationRepository;
import com.kashi.portal.ai.service.ConversationMemoryService;
import com.kashi.portal.ai.service.GeminiAiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final GeminiAiService geminiAiService;
    private final ConversationMemoryService conversationMemoryService;
    private final AiConversationRepository conversationRepository;

    public AiChatController(GeminiAiService geminiAiService, ConversationMemoryService conversationMemoryService, AiConversationRepository conversationRepository) {
        this.geminiAiService = geminiAiService;
        this.conversationMemoryService = conversationMemoryService;
        this.conversationRepository = conversationRepository;
    }

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(
            @Valid @RequestBody AiChatRequest request
    ) {
        String conversationId = request.conversationId();

        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        String reply = geminiAiService.generateReply(
                conversationId,
                request.message()
        );

        return ResponseEntity.ok(
                new AiChatResponse(reply, conversationId)
        );
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<AiConversationResponse>> getConversations() {
        return ResponseEntity.ok(
                conversationMemoryService.getAllConversations()
        );
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<AiChatMessageResponse>> getConversationMessages(
            @PathVariable String conversationId
    ) {
        boolean exists = conversationRepository
                .existsByConversationId(conversationId);

        if (!exists) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                conversationMemoryService.getConversationMessages(conversationId)
        );
    }

    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable String conversationId
    ) {
        boolean deleted = conversationMemoryService
                .deleteConversation(conversationId);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }


}
