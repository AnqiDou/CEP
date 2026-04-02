package com.example.cep_backend.config;

import com.example.cep_backend.message.ws.MessageWebSocketAuthInterceptor;
import com.example.cep_backend.message.ws.MessageWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;

@Configuration
@EnableWebSocket
public class MessageWebSocketConfig implements WebSocketConfigurer {
    private final MessageWebSocketHandler messageWebSocketHandler;
    private final MessageWebSocketAuthInterceptor messageWebSocketAuthInterceptor;

    @Value("${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
    private String allowedOriginPatterns;

    public MessageWebSocketConfig(
            MessageWebSocketHandler messageWebSocketHandler,
            MessageWebSocketAuthInterceptor messageWebSocketAuthInterceptor) {
        this.messageWebSocketHandler = messageWebSocketHandler;
        this.messageWebSocketAuthInterceptor = messageWebSocketAuthInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(messageWebSocketHandler, "/ws/messages")
                .addInterceptors(messageWebSocketAuthInterceptor)
                .setAllowedOriginPatterns(parseAllowedOriginPatterns());
    }

    private String[] parseAllowedOriginPatterns() {
        return Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toArray(String[]::new);
    }
}
