package com.example.cep_backend.message.ws;

import com.example.cep_backend.message.dto.MessageSendRequest;
import com.example.cep_backend.admin.service.AdminService;
import com.example.cep_backend.message.service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

@Component
public class MessageWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final MessageService messageService;
    private final AdminService adminService;
    private final MessageWebSocketSessionRegistry sessionRegistry;
    private final MessageWebSocketNotifier notifier;

    public MessageWebSocketHandler(
            ObjectMapper objectMapper,
            MessageService messageService,
            AdminService adminService,
            MessageWebSocketSessionRegistry sessionRegistry,
            MessageWebSocketNotifier notifier) {
        this.objectMapper = objectMapper;
        this.messageService = messageService;
        this.adminService = adminService;
        this.sessionRegistry = sessionRegistry;
        this.notifier = notifier;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = resolveUserId(session);
        if (userId == null || userId <= 0) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("未授权"));
            return;
        }
        sessionRegistry.register(userId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = resolveUserId(session);
        sessionRegistry.unregister(userId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = resolveUserId(session);
        if (userId == null || userId <= 0) {
            sendError(session, "登录状态已失效，请重新登录");
            return;
        }

        MessageWsSendRequest payload;
        try {
            payload = objectMapper.readValue(message.getPayload(), MessageWsSendRequest.class);
        } catch (Exception ex) {
            sendError(session, "消息格式不正确");
            return;
        }

        String action = payload.action() == null ? "" : payload.action().trim().toUpperCase();
        if ("PING".equals(action)) {
            session.sendMessage(new TextMessage("{\"eventType\":\"PONG\"}"));
            return;
        }

        if ("SEND_SUPPORT_MESSAGE".equals(action)) {
            try {
                adminService.appendUserSupportMessage(userId, payload.text());
            } catch (RuntimeException ex) {
                sendError(session, ex.getMessage() == null ? "发送失败" : ex.getMessage());
            }
            return;
        }

        if (!"SEND_MESSAGE".equals(action)) {
            sendError(session, "不支持的消息操作");
            return;
        }

        try {
            MessageService.MessageDispatchResult result = messageService.sendMessage(
                    userId,
                    new MessageSendRequest(payload.conversationId(), payload.text(), payload.imageUrl()));
            notifier.notifyMessageCreated(result);
        } catch (RuntimeException ex) {
            sendError(session, ex.getMessage() == null ? "发送失败" : ex.getMessage());
        }
    }

    private Long resolveUserId(WebSocketSession session) {
        Object value = session.getAttributes().get(MessageWebSocketAuthInterceptor.ATTR_USER_ID);
        if (value instanceof Long v) {
            return v;
        }
        if (value instanceof Integer v) {
            return v.longValue();
        }
        return null;
    }

    private void sendError(WebSocketSession session, String errorMessage) throws Exception {
        if (!session.isOpen()) {
            return;
        }
        String payload = objectMapper.writeValueAsString(Map.of(
                "eventType", "ERROR",
                "message", errorMessage == null ? "发送失败" : errorMessage));
        session.sendMessage(new TextMessage(payload));
    }

    public record MessageWsSendRequest(
            String action,
            Long conversationId,
            String text,
            String imageUrl) {
    }
}
