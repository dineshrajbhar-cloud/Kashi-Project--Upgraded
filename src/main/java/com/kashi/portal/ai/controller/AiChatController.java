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

import com.kashi.portal.user.User;
import com.kashi.portal.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final GeminiAiService geminiAiService;
    private final ConversationMemoryService conversationMemoryService;
    private final AiConversationRepository conversationRepository;
    private final UserRepository userRepository;

    public AiChatController(GeminiAiService geminiAiService, ConversationMemoryService conversationMemoryService, AiConversationRepository conversationRepository, UserRepository userRepository) {
        this.geminiAiService = geminiAiService;
        this.conversationMemoryService = conversationMemoryService;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(
            @Valid @RequestBody AiChatRequest request,
            Authentication authentication
    ) {
        String conversationId = request.conversationId();

        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        String reply;

        // Authenticated user
        if (authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {

            User user = getAuthenticatedUser(authentication);

            reply = geminiAiService.generateReply(
                    conversationId,
                    user.getId(),
                    request.message()
            );

        } else {
            // Guest user
            reply = geminiAiService.generateGuestReply(
                    conversationId,
                    request.message()
            );
        }

        return ResponseEntity.ok(
                new AiChatResponse(reply, conversationId)
        );
    }

//    @PostMapping("/chat")
//    public ResponseEntity<AiChatResponse> chat(
//            @Valid @RequestBody AiChatRequest request, Authentication authentication
//    ) {
//        User user = getAuthenticatedUser(authentication);
//        String conversationId = request.conversationId();
//
//        if (conversationId == null || conversationId.isBlank()) {
//            conversationId = UUID.randomUUID().toString();
//        }
//
//        String reply = geminiAiService.generateReply(
//                conversationId,
//                user.getId(),
//                request.message()
//        );
//
//        return ResponseEntity.ok(
//                new AiChatResponse(reply, conversationId)
//        );
//    }

    @GetMapping("/conversations")
    public ResponseEntity<List<AiConversationResponse>> getConversations(
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);

        return ResponseEntity.ok(
                conversationMemoryService.getAllConversations(user.getId())
        );
    }

//    @GetMapping("/conversations")
//    public ResponseEntity<List<AiConversationResponse>> getConversations() {
//        return ResponseEntity.ok(
//                conversationMemoryService.getAllConversations()
//        );
//    }


    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<AiChatMessageResponse>> getConversationMessages(
            @PathVariable String conversationId,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);

        return ResponseEntity.ok(
                conversationMemoryService.getConversationMessages(
                        conversationId,
                        user.getId()
                )
        );
    }

//    @GetMapping("/conversations/{conversationId}/messages")
//    public ResponseEntity<List<AiChatMessageResponse>> getConversationMessages(
//            @PathVariable String conversationId
//    ) {
//        boolean exists = conversationRepository
//                .existsByConversationId(conversationId);
//
//        if (!exists) {
//            return ResponseEntity.notFound().build();
//        }
//
//        return ResponseEntity.ok(
//                conversationMemoryService.getConversationMessages(conversationId)
//        );
//    }


    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable String conversationId,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);

        boolean deleted = conversationMemoryService.deleteConversation(
                conversationId,
                user.getId()
        );

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }


//    @DeleteMapping("/conversations/{conversationId}")
//    public ResponseEntity<Void> deleteConversation(
//            @PathVariable String conversationId
//    ) {
//        boolean deleted = conversationMemoryService
//                .deleteConversation(conversationId);
//
//        if (!deleted) {
//            return ResponseEntity.notFound().build();
//        }
//
//        return ResponseEntity.noContent().build();
//    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"
                ));
    }


}
