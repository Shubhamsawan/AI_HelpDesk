package com.helpdesk.help_desk_backened.Controller;

import com.helpdesk.help_desk_backened.service.AIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/v1/ai")
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }


    @Operation(summary = "Get AI response")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI response returned")
    })
    @PostMapping
    public ResponseEntity<String> getResponse(@RequestBody String query, @RequestHeader(value = "ConversationId",required = false) String conversationId){

        return ResponseEntity.ok(aiService.getResponseFromAssistant(query,conversationId));
    }

    @Operation(summary = "Get AI response")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI response returned")
    })
    @PostMapping(value = "/stream")
    public  String getAssistantResponse(@RequestBody String query, @RequestHeader(value = "ConversationId",required = false) String conversationId){

        return aiService.getAssistantResponse(query,conversationId);
    }
}
