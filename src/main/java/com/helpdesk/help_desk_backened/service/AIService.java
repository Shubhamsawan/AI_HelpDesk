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
import org.springframework.web.client.*;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@Getter
@Setter
public class AIService {

//    @Value("${spring.ai.google.genai.project-id}")
//    private String projectId;

//    private final ChatClient chatClient;
//
//    public AIService(TicketDatabaseTool ticketDatabaseTool, ChatClient chatClient) {
//        this.ticketDatabaseTool = ticketDatabaseTool;
//        this.chatClient = chatClient;
//    }
//
//    public String getResponseFromAssistant(String query) {
//        try {
//            ChatResponse response = chatClient
//                    .prompt()
//                    .tools(ticketDatabaseTool)
//                    .user(query)
//                    .call()
//                    .chatResponse();
//
//            if (response != null && !response.getResults().isEmpty()) {
//                return response.getResult().getOutput().getText();
//            } else {
//                return "No valid response from Gemini.";
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "Error contacting Gemini: " + e.getMessage();
//        }
//    }


    private final RestTemplate restTemplate;
    private final String geminiApiUrl;
    private final String geminiApiKey;
    private final TicketDatabaseTool ticketDatabaseTool;
    private final ChatMemoryService chatMemoryService;

    @Value("classpath:/helpdesk-system.st")
    private Resource systemPromptResources;
    public AIService(RestTemplate restTemplate, @Value("${gemini.api.url}") String geminiApiUrl,
                     @Value("${gemini.api.key}") String geminiApiKey, TicketDatabaseTool ticketDatabaseTool, ChatMemoryService chatMemoryService) {
        this.restTemplate = restTemplate;
        this.geminiApiUrl = geminiApiUrl;
        this.geminiApiKey = geminiApiKey;
        this.ticketDatabaseTool = ticketDatabaseTool;
        this.chatMemoryService = chatMemoryService;
    }

//    public String getResponseFromAssistant(String query) {
//        try {
//            // Load your system prompt file (acts like .system())
//            String systemPrompt;
//            try (InputStream inputStream = systemPromptResources.getInputStream()) {
//                systemPrompt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
//                System.err.println("systemPrompt "+systemPrompt);
//            }
//
//            // Preprocess the query and enrich with DB info
//            String context = enrichQueryWithTicketData(query);
//
//            // Combine system + user query
//            String combinedPrompt = systemPrompt + "\n\nUser query:\n" + context;
//
//            Map<String, Object> body = Map.of(
//                    "contents", List.of(
//                            Map.of("parts", List.of(Map.of("text", combinedPrompt)))
//                    )
//            );
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//
//            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
//            String url = geminiApiUrl + "?key=" + geminiApiKey;
//
//            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
//
//            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
//                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
//                if (candidates != null && !candidates.isEmpty()) {
//                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
//                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
//                    return parts.get(0).get("text").toString();
//                }
//            }
//
//            return "No valid response from Gemini.";
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "Error contacting Gemini: " + e.getMessage();
//        }
//    }

    public String getResponseFromAssistant(String query) {
        try {
            // Load system prompt
            String systemPrompt;
            try (InputStream inputStream = systemPromptResources.getInputStream()) {
                systemPrompt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }

            // Add user query to memory
            chatMemoryService.addMessage(query, "User", query);

            // Enrich query with ticket data
            String enrichedQuery = enrichQueryWithTicketData(query);

            // Build full context (system + memory + new query)
            String conversationContext = chatMemoryService.getConversationContext(query);
            String combinedPrompt = systemPrompt + "\n\nPrevious conversation:\n" + conversationContext +
                    "\n\nNew user message:\n" + enrichedQuery;

            // Prepare Gemini API request
            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", combinedPrompt)))
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            String url = geminiApiUrl + "?key=" + geminiApiKey;

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> candidates =
                        (List<Map<String, Object>>) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    String aiResponse = parts.get(0).get("text").toString();

                    // Store AI response in memory
                    chatMemoryService.addMessage(query, "Assistant", aiResponse);

                    return aiResponse;
                }
            }

            return "No valid response from Gemini.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error contacting Gemini: " + e.getMessage();
        }
    }


    /**
     * Add ticket info from your database to enrich Gemini's context.
     */
    private String enrichQueryWithTicketData(String query) {
        String lower = query.toLowerCase();
        System.out.println("lower " + lower);
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

                // Check if similar ticket exists
                Ticket existingTicket = ticketDatabaseTool.getTicketByUserName(username);
                if (existingTicket != null &&
                        existingTicket.getSummary().equalsIgnoreCase(summary)) {
                    return "A similar ticket already exists: " + existingTicket;
                }

                Ticket newTicket = new Ticket();
                newTicket.setUserName(username);
                newTicket.setEmail(email);
                newTicket.setSummary(summary);
                newTicket.setDescription(description);
                newTicket.setCategory(category);
                newTicket.setPriority(priority);
                newTicket.setStatus(Status.OPEN);

                Ticket savedTicket = ticketDatabaseTool.createTicketTool(newTicket);

                return "✅ New ticket created successfully:\n" +
                        "ID: " + savedTicket.getId() + "\n" +
                        "Summary: " + savedTicket.getSummary() + "\n" +
                        "Priority: " + savedTicket.getPriority() + "\n" +
                        "Category: " + savedTicket.getCategory() + "\n" +
                        "Email: " + savedTicket.getEmail() + "\n" +
                        "Status: " + savedTicket.getStatus() + "\n" +
                        "Created On: " + savedTicket.getCreatedOn();
            }

        } catch (Exception e) {
            System.out.println("Error using TicketDatabaseTool: " + e.getMessage());
        }

        // Default
        return query;
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
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-z]{2,6}")
                        .matcher(query);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private String extractCategoryFromQuery(String query) {
        if (query.contains("network")) return "Network";
        if (query.contains("hardware")) return "Hardware";
        if (query.contains("software")) return "Software";
        return "General";
    }

    private String extractPriorityFromQuery(String query) {
        if (query.contains("urgent")) return "URGENT";
        if (query.contains("high")) return "HIGH";
        if (query.contains("medium")) return "MEDIUM";
        if (query.contains("low")) return "LOW";
        return "LOW";
    }





//    public String getResponseFromAssistant(String query){
//        return this.chatClient.prompt()
//                prompt()
//                        .tools(ticketDatabaseTool)
    //.system(systemPromptResources)
//                .user(query)
//                .call()
//                .content();
//    }
}
