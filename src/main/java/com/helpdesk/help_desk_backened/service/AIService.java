package com.helpdesk.help_desk_backened.service;

import com.helpdesk.help_desk_backened.entity.Ticket;
import com.helpdesk.help_desk_backened.enums.Priority;
import com.helpdesk.help_desk_backened.enums.Status;
import com.helpdesk.help_desk_backened.tools.TicketDatabaseTool;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@Getter
@Setter
public class AIService {

    private final RestTemplate restTemplate;
    private final String groqApiUrl;
    private final String groqApiKey;
    private final TicketDatabaseTool ticketDatabaseTool;
    private final ChatMemoryService chatMemoryService;

    @Value("classpath:/helpdesk-system.st")
    private Resource systemPromptResources;

    public AIService(RestTemplate restTemplate,
                     @Value("${groq.api.url}") String groqApiUrl,
                     @Value("${groq.api.key}") String groqApiKey,
                     TicketDatabaseTool ticketDatabaseTool,
                     ChatMemoryService chatMemoryService) {

        this.restTemplate = restTemplate;
        this.groqApiUrl = groqApiUrl;
        this.groqApiKey = groqApiKey;
        this.ticketDatabaseTool = ticketDatabaseTool;
        this.chatMemoryService = chatMemoryService;
    }

    /**
     * Main AI response generator (Groq API)
     */
    public String getResponseFromAssistant(String query) {
        try {
            // Load system prompt
            String systemPrompt;
            try (InputStream inputStream = systemPromptResources.getInputStream()) {
                systemPrompt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }

            // Save user msg
            chatMemoryService.addMessage(query, "User", query);

            // Enrich with DB data
            String enrichedQuery = enrichQueryWithTicketData(query);

            // Add memory
            String memory = chatMemoryService.getConversationContext(query);

            // Final prompt
            String finalPrompt = systemPrompt +
                    "\n\nPrevious conversation:\n" + memory +
                    "\n\nUser message:\n" + enrichedQuery;

            // Build Groq Request
            Map<String, Object> body = Map.of(
                    "model", "llama-3.1-8b-instant",
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", finalPrompt)
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + groqApiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(groqApiUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {

                List<Map<String, Object>> choices =
                        (List<Map<String, Object>>) response.getBody().get("choices");

                Map<String, Object> msg =
                        (Map<String, Object>) choices.get(0).get("message");

                String aiResponse = msg.get("content").toString();

                // save AI response
                chatMemoryService.addMessage(query, "Assistant", aiResponse);

                return aiResponse;
            }

            return "No response from AI.";
        } catch (Exception e) {
            e.printStackTrace();
            return "AI Error: " + e.getMessage();
        }
    }

    /**
     * Your original ticket enrichment logic (unchanged)
     */
    private String enrichQueryWithTicketData(String query) {
        String lower = query.toLowerCase();

        try {
            if (lower.contains("create ticket")) {

                String username = extractUsernameFromQuery(lower);
                String summary = extractSummaryFromQuery(lower);
                String description = extractDescriptionFromQuery(lower);
                String email = extractEmailFromQuery(lower);
                String category = extractCategoryFromQuery(lower);
                String priorityStr = extractPriorityFromQuery(lower);

                if (email == null) {
                    return "Please provide your email address so I can create the ticket.";
                }

                Priority priority = Priority.valueOf(
                        (priorityStr != null ? priorityStr.toUpperCase() : "LOW")
                );

                // Check duplicate ticket
                Ticket existing = ticketDatabaseTool.getTicketByUserName(username);
                if (existing != null &&
                        existing.getSummary().equalsIgnoreCase(summary)) {
                    return "A similar ticket exists: " + existing;
                }

                Ticket newTicket = new Ticket();
                newTicket.setUserName(username);
                newTicket.setEmail(email);
                newTicket.setSummary(summary);
                newTicket.setDescription(description);
                newTicket.setCategory(category);
                newTicket.setPriority(priority);
                newTicket.setStatus(Status.OPEN);

                Ticket saved = ticketDatabaseTool.createTicketTool(newTicket);

                return "New Ticket Created:\n" +
                        "ID: " + saved.getId() + "\n" +
                        "Summary: " + saved.getSummary() + "\n" +
                        "Priority: " + saved.getPriority() + "\n" +
                        "Category: " + saved.getCategory() + "\n" +
                        "Email: " + saved.getEmail() + "\n" +
                        "Status: " + saved.getStatus() + "\n" +
                        "Created On: " + saved.getCreatedOn();
            }

        } catch (Exception e) {
            System.out.println("Error using TicketDatabaseTool: " + e.getMessage());
        }

        return query;
    }

    // --------------------- Extractors (unchanged) -----------------------
    private String extractPriorityFromQuery(String query) {
        if (query == null || query.isEmpty()) return "MEDIUM";

        query = query.toLowerCase();

        if (query.contains("urgent") || query.contains("immediately") || query.contains("asap")) {
            return "URGENT";
        }
        if (query.contains("high") || query.contains("critical") || query.contains("important")) {
            return "HIGH";
        }
        if (query.contains("medium") || query.contains("normal")) {
            return "MEDIUM";
        }
        if (query.contains("low") || query.contains("not important")) {
            return "LOW";
        }

        return "MEDIUM"; // default priority
    }

    private String extractCategoryFromQuery(String query) {
        if (query == null || query.isEmpty()) return "others";

        query = query.toLowerCase();

        if (query.contains("wifi") || query.contains("internet") || query.contains("network")) {
            return "network";
        }
        if (query.contains("software") || query.contains("app") || query.contains("application")) {
            return "software";
        }
        if (query.contains("computer") || query.contains("keyboard") || query.contains("mouse")
                || query.contains("laptop") || query.contains("hardware")) {
            return "hardware";
        }
        if (query.contains("login") || query.contains("password") || query.contains("account")) {
            return "account";
        }
        if (query.contains("payment") || query.contains("bill") || query.contains("invoice")) {
            return "payment";
        }

        return "others";
    }

    private String extractUsernameFromQuery(String query) {
        int index = query.indexOf("username");
        if (index != -1) {
            String[] parts = query.substring(index + 8).trim().split("\\s+");
            if (parts.length > 0) return parts[0].replaceAll("[^a-zA-Z0-9]", "");
        }
        return "GuestUser";
    }

    private String extractSummaryFromQuery(String query) {
        int index = query.indexOf("summary");
        if (index != -1) {
            return query.substring(index + 7).replaceAll("description.*", "").trim();
        }
        return "General issue reported by user";
    }

    private String extractDescriptionFromQuery(String query) {
        int index = query.indexOf("description");
        if (index != -1) {
            return query.substring(index + 11).replaceAll("priority.*", "").trim();
        }
        return "No detailed description provided.";
    }

    private String extractEmailFromQuery(String query) {
        var matcher = java.util.regex.Pattern
                .compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-z]{2,6}")
                .matcher(query);
        return query;
    }

}