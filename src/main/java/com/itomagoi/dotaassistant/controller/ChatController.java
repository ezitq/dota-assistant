package com.itomagoi.dotaassistant.controller;

import com.itomagoi.dotaassistant.service.OpenDotaService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatClient chatClient;
    private final OpenDotaService openDotaService;

    // Інжектимо і ChatClient, і наш сервіс
    public ChatController(ChatClient.Builder chatClientBuilder, OpenDotaService openDotaService) {
        this.chatClient = chatClientBuilder.build();
        this.openDotaService = openDotaService;
    }

    @GetMapping
    public String chat(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                // Передаємо екземпляр сервісу, і Spring AI сам знайде метод @Tool
                .tools(openDotaService)
                .call()
                .content();
    }
}