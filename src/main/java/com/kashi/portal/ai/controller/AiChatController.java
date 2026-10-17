package com.kashi.portal.ai.controller;

import com.kashi.portal.ai.dto.AiChatRequest;
import com.kashi.portal.ai.dto.AiChatResponse;
import com.kashi.portal.ai.service.GeminiAiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final GeminiAiService geminiAiService;

    public AiChatController(GeminiAiService geminiAiService) {
        this.geminiAiService = geminiAiService;
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

//    @PostMapping("/chat")
//    public ResponseEntity<AiChatResponse> chat(
//            @Valid @RequestBody AiChatRequest request
//    ) {
//        String reply = geminiAiService.generateReply(request.message());
//
//        return ResponseEntity.ok(
//                new AiChatResponse(reply)
//        );
//    }
}
