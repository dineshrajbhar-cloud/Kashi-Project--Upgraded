package com.kashi.portal.ai.service;

import com.kashi.portal.common.AiServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeminiAiService {

    private final RestClient restClient;
    private final ConversationMemoryService conversationMemoryService;
    private final String model;

    // Temporary conversation history for guest users.
    private final Map<String, List<ConversationMemoryService.ChatMessage>>
            guestHistories = new ConcurrentHashMap<>();

    public GeminiAiService(
            @Value("${app.ai.groq.api-key}") String apiKey,
            @Value("${app.ai.groq.model}") String model,
            ConversationMemoryService conversationMemoryService
    ) {
        this.model = model;
        this.conversationMemoryService = conversationMemoryService;

        this.restClient = RestClient.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    // Authenticated user's chat.
    public String generateReply(
            String conversationId,
            Long userId,
            String message
    ) {
        List<ConversationMemoryService.ChatMessage> history =
                conversationMemoryService.getHistory(conversationId, userId);

        String reply = callGroq(history, message);

        conversationMemoryService.addExchange(
                conversationId,
                userId,
                message,
                reply
        );

        return reply;
    }

    // Guest user's chat.
    public String generateGuestReply(
            String conversationId,
            String message
    ) {
        List<ConversationMemoryService.ChatMessage> history =
                guestHistories.computeIfAbsent(
                        conversationId,
                        key -> new ArrayList<>()
                );

        synchronized (history) {
            // Create a snapshot of previous messages.
            List<ConversationMemoryService.ChatMessage> previousMessages =
                    List.copyOf(history);

            String reply = callGroq(previousMessages, message);

            // Save in memory only; do not persist guest messages to the database.
            history.add(new ConversationMemoryService.ChatMessage(
                    "user", message
            ));

            history.add(new ConversationMemoryService.ChatMessage(
                    "assistant", reply
            ));

            // Keep only the latest 10 messages.
            if (history.size() > 10) {
                history.subList(0, history.size() - 10).clear();
            }

            return reply;
        }
    }

    private String callGroq(
            List<ConversationMemoryService.ChatMessage> history,
            String message
    ) {
        String systemInstruction = """
                You are the official AI tourism assistant for Kashi (Varanasi), India.

                YOUR ROLE:
                Help tourists plan meaningful and accurate visits to Kashi.

                CONVERSATION MEMORY:
                1. Use previous messages to understand the user's context.
                2. Remember trip duration, interests, budget, and preferences
                   when provided in this conversation.
                3. Understand follow-up messages such as "make an itinerary",
                   "what about the second day", and "suggest nearby places".
                4. Never claim the user provided information absent from the conversation.
                5. If essential information is missing, ask a relevant question.

                LANGUAGE:
                1. Understand Hindi, English, and natural Hinglish.
                2. Reply in the language used by the user.
                3. Keep responses friendly, clear, and practical.

                ACCURACY RULES:
                1. Never invent temples, ghats, monuments, restaurants,
                   or tourist attractions.
                2. Recognized attractions include Shri Kashi Vishwanath Temple,
                   Dashashwamedh Ghat, Assi Ghat, Manikarnika Ghat, Sarnath,
                   Sankat Mochan Hanuman Temple, Banaras Hindu University,
                   and Bharat Kala Bhavan.
                3. Never invent opening hours, ticket prices, booking availability,
                   travel schedules, or real-time information.
                4. Do not claim to have live browsing or booking access.

                RESPONSE FORMATTING:
                1. Use Markdown for readable responses.
                2. Use headings for longer answers.
                3. Use numbered lists for steps and bullet lists for tips.
                4. Keep paragraphs short.
                5. Never output raw HTML.
                6. Answer short questions concisely.
                
                Conversation style rules:
                - Keep responses concise, friendly, and conversational.
                - Ask at most one or two relevant follow-up questions at a time.
                - Do not repeatedly ask questions the user has already answered.
                - Do not turn every response into a numbered list or bullet list.
                - Prefer short paragraphs for normal conversation.
                - Use bullet points only when they genuinely improve readability.
                - Avoid unnecessary blank lines between paragraphs and list items.
                - If the user shares their name, acknowledge it naturally and continue the conversation.
                - Do not ask for trip details unless they are relevant to the user's request.
                """;

        List<Map<String, String>> messages = new ArrayList<>();

        messages.add(Map.of(
                "role", "system",
                "content", systemInstruction
        ));

        for (ConversationMemoryService.ChatMessage chatMessage : history) {
            messages.add(Map.of(
                    "role", chatMessage.role(),
                    "content", chatMessage.content()
            ));
        }

        messages.add(Map.of(
                "role", "user",
                "content", message
        ));

        try {
            JsonNode response = restClient.post()
                    .uri("/chat/completions")
                    .body(Map.of(
                            "model", model,
                            "messages", messages,
                            "temperature", 0.4,
                            "max_tokens", 700
                    ))
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (request, clientResponse) -> {
                                int statusCode =
                                        clientResponse.getStatusCode().value();

                                String errorBody = new String(
                                        clientResponse.getBody().readAllBytes(),
                                        StandardCharsets.UTF_8
                                );

                                if (statusCode == 429
                                        || statusCode == 502
                                        || statusCode == 503
                                        || statusCode == 504) {
                                    throw new AiServiceUnavailableException(
                                            "Groq service is temporarily unavailable. HTTP "
                                                    + statusCode
                                    );
                                }

                                throw new IllegalStateException(
                                        "Groq API error: HTTP "
                                                + statusCode + " - " + errorBody
                                );
                            }
                    )
                    .body(JsonNode.class);

            if (response == null) {
                throw new IllegalStateException(
                        "Groq returned an empty response."
                );
            }

            JsonNode textNode =
                    response.at("/choices/0/message/content");

            if (textNode.isMissingNode() || textNode.isNull()) {
                throw new IllegalStateException(
                        "Groq did not return a text response."
                );
            }

            return textNode.asText();

        } catch (AiServiceUnavailableException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to generate an AI response.",
                    exception
            );
        }
    }
}


//package com.kashi.portal.ai.service;
//
//import com.kashi.portal.common.AiServiceUnavailableException;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpStatusCode;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestClient;
//import tools.jackson.databind.JsonNode;
//
//import java.nio.charset.StandardCharsets;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//@Service
//public class GeminiAiService {
//
//    private final RestClient restClient;
//    private final ConversationMemoryService conversationMemoryService;
//    private final String model;
//
//    public GeminiAiService(
//            @Value("${app.ai.groq.api-key}") String apiKey,
//            @Value("${app.ai.groq.model}") String model,
//            ConversationMemoryService conversationMemoryService
//    ) {
//        this.model = model;
//        this.conversationMemoryService = conversationMemoryService;
//
//        this.restClient = RestClient.builder()
//                .baseUrl("https://api.groq.com/openai/v1")
//                .defaultHeader("Authorization", "Bearer " + apiKey)
//                .build();
//    }
//
//    public String generateReply(String conversationId, Long userId, String message) {
//
//        String systemInstruction = """
//                You are the official AI tourism assistant for Kashi (Varanasi), India.
//
//                YOUR ROLE:
//                Help tourists plan meaningful and accurate visits to Kashi.
//
//                CONVERSATION MEMORY:
//                1. Use previous messages to understand the user's context.
//                2. Remember trip duration, interests, budget, and preferences
//                   when the user has provided them in this conversation.
//                3. Understand follow-up messages such as "make an itinerary",
//                   "what about the second day", and "suggest nearby places".
//                4. Never claim the user provided information that is absent
//                   from the conversation.
//                5. If essential information is missing, ask a relevant question.
//
//                LANGUAGE:
//                1. Understand Hindi, English, and natural Hinglish.
//                2. Reply in the language used by the user.
//                3. Keep responses friendly, clear, and practical.
//
//                ACCURACY RULES:
//                1. Never invent temples, ghats, monuments, restaurants,
//                   or tourist attractions.
//                2. Mention only places you are reasonably confident exist.
//                3. Recognized attractions include:
//                   - Shri Kashi Vishwanath Temple
//                   - Dashashwamedh Ghat
//                   - Assi Ghat
//                   - Manikarnika Ghat
//                   - Sarnath
//                   - Sankat Mochan Hanuman Temple
//                   - Banaras Hindu University
//                   - Bharat Kala Bhavan
//                4. If unsure whether a place exists or is open, say you
//                   cannot verify it.
//                5. Never invent opening hours, ticket prices, booking
//                   availability, travel schedules, or real-time information.
//                6. Do not claim to have live browsing or booking access.
//
//                RESPONSE FORMATTING:
//                1. Use Markdown for readable responses.
//                2. Use headings for longer answers.
//                3. Use numbered lists for steps and bullet lists for tips.
//                4. Use bold only for important information.
//                5. Keep paragraphs short.
//                6. Never output raw HTML.
//                7. For short questions, answer concisely.
//                """;
//
//        List<ConversationMemoryService.ChatMessage> history =
//                conversationMemoryService.getHistory(conversationId, userId);
//
//
//        List<Map<String, String>> messages = new ArrayList<>();
//
//        messages.add(Map.of(
//                "role", "system",
//                "content", systemInstruction
//        ));
//
//        for (ConversationMemoryService.ChatMessage chatMessage : history) {
//            messages.add(Map.of(
//                    "role", chatMessage.role(),
//                    "content", chatMessage.content()
//            ));
//        }
//
//        messages.add(Map.of(
//                "role", "user",
//                "content", message
//        ));
//
//        try {
//            JsonNode response = restClient.post()
//                    .uri("/chat/completions")
//                    .body(Map.of(
//                            "model", model,
//                            "messages", messages,
//                            "temperature", 0.4,
//                            "max_tokens", 700
//                    ))
//                    .retrieve()
//                    .onStatus(
//                            HttpStatusCode::isError,
//                            (request, clientResponse) -> {
//
//                                int statusCode =
//                                        clientResponse.getStatusCode().value();
//
//                                String errorBody = new String(
//                                        clientResponse.getBody().readAllBytes(),
//                                        StandardCharsets.UTF_8
//                                );
//
//                                if (statusCode == 429
//                                        || statusCode == 502
//                                        || statusCode == 503
//                                        || statusCode == 504) {
//
//                                    throw new AiServiceUnavailableException(
//                                            "Groq service is temporarily unavailable. HTTP "
//                                                    + statusCode
//                                    );
//                                }
//
//                                throw new IllegalStateException(
//                                        "Groq API error: HTTP "
//                                                + statusCode
//                                                + " - "
//                                                + errorBody
//                                );
//                            }
//                    )
//                    .body(JsonNode.class);
//
//            if (response == null) {
//                throw new IllegalStateException(
//                        "Groq returned an empty response."
//                );
//            }
//
//            JsonNode textNode =
//                    response.at("/choices/0/message/content");
//
//            if (textNode.isMissingNode() || textNode.isNull()) {
//                throw new IllegalStateException(
//                        "Groq did not return a text response."
//                );
//            }
//
//            String reply = textNode.asText();
//
//            // Save both messages in a single database transaction.
//            conversationMemoryService.addExchange(
//                    conversationId,
//                    userId,
//                    message,
//                    reply
//            );
//
//            return reply;
//
//        } catch (AiServiceUnavailableException exception) {
//            throw exception;
//        } catch (Exception exception) {
//            throw new IllegalStateException(
//                    "Unable to generate an AI response.",
//                    exception
//            );
//        }
//    }
//
//}
