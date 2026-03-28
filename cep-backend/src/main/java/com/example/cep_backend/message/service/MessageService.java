package com.example.cep_backend.message.service;

import com.example.cep_backend.auth.BusinessException;
import com.example.cep_backend.message.dto.MessageConversationDto;
import com.example.cep_backend.message.dto.MessageItemDto;
import com.example.cep_backend.message.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {
    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public List<MessageConversationDto> getConversations(Long userId, String filter) {
        return messageRepository.findConversations(userId, normalizeFilter(filter));
    }

    public List<MessageItemDto> getConversationMessages(Long userId, Long conversationId) {
        if (conversationId == null || conversationId <= 0) {
            throw new BusinessException("会话参数无效");
        }
        if (!messageRepository.existsConversationForUser(conversationId, userId)) {
            throw new BusinessException("会话不存在");
        }
        return messageRepository.findConversationMessages(conversationId, userId);
    }

    private String normalizeFilter(String filter) {
        String normalized = filter == null ? "all" : filter.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return "all";
        }
        if (!"all".equals(normalized) && !"read".equals(normalized) && !"unread".equals(normalized)) {
            throw new BusinessException("消息筛选参数无效");
        }
        return normalized;
    }
}
