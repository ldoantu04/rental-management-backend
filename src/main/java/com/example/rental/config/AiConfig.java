package com.example.rental.config;

import com.example.rental.service.ai.ChatQueryTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    /**
     * Single shared ChatClient with the AI query tools pre-registered exactly once.
     * Re-invoking {@code defaultTools(...)} on the same builder instance would
     * append the tools, causing duplicate-name errors on the second request.
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel, ChatQueryTools queryTools) {
        return ChatClient.builder(chatModel)
                .defaultTools(queryTools)
                .build();
    }
}
