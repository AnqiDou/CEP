package com.example.cep_backend.message.ws;

import com.example.cep_backend.message.dto.MessageConversationDto;
import com.example.cep_backend.message.dto.MessageItemDto;
import com.example.cep_backend.message.dto.MessageWsEventDto;
import com.example.cep_backend.message.service.MessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class MessageWebSocketNotifier {
    private final ObjectMapper objectMapper;
    private final MessageWebSocketSessionRegistry sessionRegistry;

    public MessageWebSocketNotifier(ObjectMapper objectMapper, MessageWebSocketSessionRegistry sessionRegistry) {
        this.objectMapper = objectMapper;
        this.sessionRegistry = sessionRegistry;
    }

    public void notifyMessageCreated(MessageService.MessageDispatchResult result) {
        if (result == null) {
            return;
        }

        sendConversationEvent(result.senderUserId(), "MESSAGE_CREATED", result.senderConversation(),
                result.senderMessage());
        sendConversationEvent(result.receiverUserId(), "MESSAGE_CREATED", result.receiverConversation(),
                result.receiverMessage());
    }

    public void sendConversationEvent(Long userId, String eventType, MessageConversationDto conversation,
            MessageItemDto message) {
        if (userId == null || userId <= 0) {
            return;
        }
        MessageWsEventDto event = new MessageWsEventDto(eventType, conversation, message);
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            return;
        }

        TextMessage textMessage = new TextMessage(payload);
        for (WebSocketSession session : sessionRegistry.sessionsOf(userId)) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(textMessage);
            } catch (Exception ignored) {
                // ignore broken session
            }
        }
    }
}
