package com.kashi.portal.ai.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationMemoryService {

    private static final int MAX_HISTORY_MESSAGES = 10;

    private final Map<String, List<ChatMessage>> conversations =
            new ConcurrentHashMap<>();

    public List<ChatMessage> getHistory(String conversationId) {
        List<ChatMessage> history = conversations.get(conversationId);

        if (history == null) {
            return new ArrayList<>();
        }

        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    public void addMessage(String conversationId, String role, String content) {
        List<ChatMessage> history = conversations.computeIfAbsent(
                conversationId,
                key -> new ArrayList<>()
        );

        synchronized (history) {
            history.add(new ChatMessage(role, content));

            if (history.size() > MAX_HISTORY_MESSAGES) {
                int excess = history.size() - MAX_HISTORY_MESSAGES;
                history.subList(0, excess).clear();
            }
        }
    }

    public record ChatMessage(String role, String content) {
    }
}