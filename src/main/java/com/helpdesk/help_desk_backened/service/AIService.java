package com.helpdesk.help_desk_backened.service;

import com.helpdesk.help_desk_backened.tools.TicketDatabaseTool;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
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

    @Value("classpath:/helpdesk-system.st")
    private Resource systemPromptResources;
    public AIService(RestTemplate restTemplate, @Value("${gemini.api.url}") String geminiApiUrl,
                     @Value("${gemini.api.key}") String geminiApiKey, TicketDatabaseTool ticketDatabaseTool) {
        this.restTemplate = restTemplate;
        this.geminiApiUrl = geminiApiUrl;
        this.geminiApiKey = geminiApiKey;
        this.ticketDatabaseTool = ticketDatabaseTool;
    }

    public String getResponseFromAssistant(String query) {
        try {
            // Load your system prompt file (acts like .system())
            String systemPrompt;
            try (InputStream inputStream = systemPromptResources.getInputStream()) {
                systemPrompt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                System.err.println("systemPrompt "+systemPrompt);
            }

            // Preprocess the query and enrich with DB info
            String context = enrichQueryWithTicketData(query);

            // Combine system + user query
            String combinedPrompt = systemPrompt + "\n\nUser query:\n" + context;

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
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    return parts.get(0).get("text").toString();
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
        System.out.println("lower "+lower);
        try {
            if (lower.contains("ticket") && lower.contains("get")) {
                // Example: "Get ticket for username John"
                String username = extractUsernameFromQuery(lower);
                if (username != null) {
                    var ticket = ticketDatabaseTool.getTicketByUserName(username);
                    if (ticket != null) {
                        return "User asked: " + query + "\nHere is the ticket info from the database:\n" + ticket.toString();
                    }
                }
            } else if (lower.contains("create ticket")) {
                // Example: "Create ticket for..."
                return "User asked to create a new ticket. " +
                        "Use this context to generate a ticket summary: " + query;
            } else if (lower.contains("update ticket")) {
                return "User asked to update a ticket. Context: " + query;
            }

        } catch (Exception e) {
            System.out.println("Error using TicketDatabaseTool: " + e.getMessage());
        }

        // Default
        return query;
    }

    private String extractUsernameFromQuery(String query) {
        // Very simple example — customize for your queries
        String[] parts = query.split(" ");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("username") && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        return null;
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
