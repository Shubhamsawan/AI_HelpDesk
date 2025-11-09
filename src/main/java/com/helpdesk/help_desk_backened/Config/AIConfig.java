package com.helpdesk.help_desk_backened.Config;

//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AIConfig {
    // Load values from application.properties
    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Bean
    public RestTemplate restTemplate() {
        // Create a RestTemplate with timeouts
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(20000);
        return new RestTemplate(factory);
    }

    @Bean
    public String geminiApiUrl() {
        return geminiApiUrl;
    }

    @Bean
    public String geminiApiKey() {
        return geminiApiKey;
    }
}
