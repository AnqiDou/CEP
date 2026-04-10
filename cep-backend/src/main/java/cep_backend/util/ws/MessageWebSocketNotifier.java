package cep_backend.util.ws;
import cep_backend.dto.MessageConversationDto;
import cep_backend.dto.MessageItemDto;
import cep_backend.dto.MessageWsEventDto;
import cep_backend.service.MessageService;
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
