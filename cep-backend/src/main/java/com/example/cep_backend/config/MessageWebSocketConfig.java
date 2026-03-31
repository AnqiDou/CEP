package com.example.cep_backend.config;

import com.example.cep_backend.message.ws.MessageWebSocketAuthInterceptor;
import com.example.cep_backend.message.ws.MessageWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class MessageWebSocketConfig implements WebSocketConfigurer {
    private final MessageWebSocketHandler messageWebSocketHandler;
    private final MessageWebSocketAuthInterceptor messageWebSocketAuthInterceptor;

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
                .setAllowedOrigins("http://localhost:5173");
    }
}
