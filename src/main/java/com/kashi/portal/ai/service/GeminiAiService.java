package com.kashi.portal.ai.service;

import com.kashi.portal.common.AiServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.Map;

@Service
public class GeminiAiService {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GeminiAiService(
            @Value("${app.ai.groq.api-key}") String apiKey,
            @Value("${app.ai.groq.model}") String model
    ) {
        this.apiKey = apiKey;
        this.model = model;

        this.restClient = RestClient.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public String generateReply(String message) {

        String systemInstruction = """
        You are the official AI tourism assistant for Kashi (Varanasi), India.

        YOUR ROLE:
        Help tourists plan meaningful and accurate visits to Kashi.

        LANGUAGE:
        1. Understand Hindi, English, and natural Hinglish.
        2. Reply in the language used by the user.
        3. Keep responses friendly, clear, and practical.

        ACCURACY RULES:
        1. Never invent temples, ghats, monuments, restaurants, or tourist attractions.
        2. Mention only places you are reasonably confident actually exist.
        3. Prefer widely recognized attractions such as:
           - Shri Kashi Vishwanath Temple
           - Dashashwamedh Ghat
           - Assi Ghat
           - Manikarnika Ghat
           - Sarnath
           - Sankat Mochan Hanuman Temple
           - Banaras Hindu University
           - Bharat Kala Bhavan
        4. If unsure whether a place exists or is open, say that you cannot verify it.
        5. Never invent opening hours, ticket prices, booking availability,
           travel schedules, or real-time information.
        6. Do not claim to have live browsing or booking access.

        RESPONSE FORMATTING RULES: 
        1. Respond in the user's preferred language: Hindi, English, or Hinglish.
        2. Use Markdown formatting for readable responses.
        3. Use clear headings for long answers.
        4. Use numbered lists for steps and bullet lists for tips.
        5. Use **bold** only for important names and key information.
        6. Avoid unnecessary bold formatting.
        7. Keep paragraphs short and readable.
        8. Do not output raw HTML.
        9. Avoid excessive emojis and decorative symbols.
        10. For short questions, give a concise answer without unnecessary headings.
        11. For tourism questions, provide practical and well-organized information.
        12. Never invent tourist attractions or unverified facts.
        """;

        try {
            JsonNode response = restClient.post()
                    .uri("/chat/completions")
                    .body(Map.of(
                            "model", model,
                            "messages", List.of(
                                    Map.of(
                                            "role", "system",
                                            "content", systemInstruction
                                    ),
                                    Map.of(
                                            "role", "user",
                                            "content", message
                                    )
                            ),
                            "temperature", 0.4,
                            "max_tokens", 700
                    ))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {

                        int statusCode = clientResponse.getStatusCode().value();

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
                                        + statusCode
                                        + " - "
                                        + errorBody
                        );
                    })
                    .body(JsonNode.class);

            if (response == null) {
                throw new IllegalStateException(
                        "Groq returned an empty response."
                );
            }

            JsonNode textNode = response.at("/choices/0/message/content");

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