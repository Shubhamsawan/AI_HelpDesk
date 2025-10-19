package com.substring.helpdesk.help_desk_backened.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@Getter
@Setter
public class AIService {

    private final ChatClient chatClient;

    public AIService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String getResponseFromAssistant(String query){
       return this.chatClient.prompt().user(query).call().content();
    }
}
