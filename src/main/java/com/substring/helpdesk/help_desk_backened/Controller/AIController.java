package com.substring.helpdesk.help_desk_backened.Controller;

import com.substring.helpdesk.help_desk_backened.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/ai")
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping
    public ResponseEntity<String> getResponse(@RequestBody String query){
        return ResponseEntity.ok(aiService.getResponseFromAssistant(query));
    }
}
