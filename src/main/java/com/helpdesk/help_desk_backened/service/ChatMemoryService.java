package com.helpdesk.help_desk_backened.service;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

@Component
public class ChatMemoryService {

    // Stores chat history per user/session
    private final Map<String, Deque<String>> memory = new HashMap<>();

    private static final int MAX_HISTORY = 5; // keep last 5 messages per user

    public void addMessage(String sessionId, String role, String message) {
        memory.putIfAbsent(sessionId, new ArrayDeque<>());
        Deque<String> history = memory.get(sessionId);

        // Save "role: message"
        history.addLast(role + ": " + message);
        if (history.size() > MAX_HISTORY * 2) { // user + ai pairs
            history.removeFirst();
        }
    }

    public String getConversationContext(String sessionId) {
        if (!memory.containsKey(sessionId)) return "";
        return String.join("\n", memory.get(sessionId));
    }


    public void clearSession(String sessionId) {
        memory.remove(sessionId);
    }
}
