package com.example.cep_backend.message.service;

import com.example.cep_backend.auth.BusinessException;
import com.example.cep_backend.message.dto.MessageConversationDto;
import com.example.cep_backend.message.dto.MessageCreateConversationRequest;
import com.example.cep_backend.message.dto.MessageItemDto;
import com.example.cep_backend.message.dto.MessageNotificationDto;
import com.example.cep_backend.message.dto.MessageSendRequest;
import com.example.cep_backend.message.repository.MessageNotificationRepository;
import com.example.cep_backend.message.repository.MessageRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final MessageNotificationRepository messageNotificationRepository;

    public MessageService(
            MessageRepository messageRepository,
            MessageNotificationRepository messageNotificationRepository) {
        this.messageRepository = messageRepository;
        this.messageNotificationRepository = messageNotificationRepository;
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

    public MessageConversationDto createOrGetConversation(Long currentUserId,
            MessageCreateConversationRequest request) {
        if (request == null) {
            throw new BusinessException("会话参数无效");
        }

        Long peerUserId = request.peerUserId();
        Long itemId = request.itemId();
        if (peerUserId == null || peerUserId <= 0 || itemId == null || itemId <= 0) {
            throw new BusinessException("会话参数无效");
        }
        if (peerUserId.equals(currentUserId)) {
            throw new BusinessException("不能与自己发起会话");
        }

        Long sellerUserId = messageRepository.findItemSellerUserId(itemId);
        if (sellerUserId == null || sellerUserId <= 0) {
            throw new BusinessException("物品不存在或卖家信息无效");
        }

        Long buyerUserId;
        if (currentUserId.equals(sellerUserId)) {
            buyerUserId = peerUserId;
        } else {
            if (!peerUserId.equals(sellerUserId)) {
                throw new BusinessException("会话对象与物品不匹配");
            }
            buyerUserId = currentUserId;
        }

        if (buyerUserId.equals(sellerUserId)) {
            throw new BusinessException("会话参数无效");
        }

        Long conversationId = messageRepository.findConversationIdByItemAndPair(itemId, buyerUserId, sellerUserId);
        if (conversationId == null) {
            try {
                conversationId = messageRepository.createConversation(itemId, buyerUserId, sellerUserId,
                        LocalDateTime.now());
            } catch (DataAccessException ex) {
                conversationId = messageRepository.findConversationIdByItemAndPair(itemId, buyerUserId, sellerUserId);
            }
        }
        if (conversationId == null) {
            throw new BusinessException("创建会话失败，请稍后重试");
        }

        MessageConversationDto conversation = messageRepository.findConversationByIdForUser(currentUserId,
                conversationId);
        if (conversation == null) {
            throw new BusinessException("会话不存在");
        }
        return conversation;
    }

    public void markConversationRead(Long userId, Long conversationId) {
        if (conversationId == null || conversationId <= 0) {
            throw new BusinessException("会话参数无效");
        }
        if (!messageRepository.existsConversationForUser(conversationId, userId)) {
            throw new BusinessException("会话不存在");
        }
        messageRepository.markConversationRead(conversationId, userId);
    }

    public MessageDispatchResult sendMessage(Long senderUserId, MessageSendRequest request) {
        if (request == null || request.conversationId() == null || request.conversationId() <= 0) {
            throw new BusinessException("会话参数无效");
        }

        String text = request.text() == null ? "" : request.text().trim();
        String imageUrl = request.imageUrl() == null ? "" : request.imageUrl().trim();
        if (text.isEmpty() && imageUrl.isEmpty()) {
            throw new BusinessException("请输入消息或选择图片");
        }
        if (text.length() > 2000) {
            throw new BusinessException("消息长度不能超过2000字");
        }
        if (imageUrl.length() > 500) {
            throw new BusinessException("图片地址过长");
        }

        MessageRepository.ConversationParticipants participants = messageRepository
                .findConversationParticipantsForUser(request.conversationId(), senderUserId);
        if (participants == null) {
            throw new BusinessException("会话不存在");
        }

        MessageRepository.DeliveryRecord delivery = messageRepository.saveMessage(request.conversationId(),
                senderUserId, text, imageUrl, LocalDateTime.now());
        if (delivery == null) {
            throw new BusinessException("消息发送失败，请稍后重试");
        }

        MessageItemDto senderMessage = messageRepository.findMessageItemForUser(delivery.messageId(),
                delivery.senderUserId());
        MessageItemDto receiverMessage = messageRepository.findMessageItemForUser(delivery.messageId(),
                delivery.receiverUserId());
        MessageConversationDto senderConversation = messageRepository.findConversationByIdForUser(
                delivery.senderUserId(),
                delivery.conversationId());
        MessageConversationDto receiverConversation = messageRepository.findConversationByIdForUser(
                delivery.receiverUserId(),
                delivery.conversationId());

        if (senderMessage == null || receiverMessage == null || senderConversation == null
                || receiverConversation == null) {
            throw new BusinessException("消息发送失败，请稍后重试");
        }

        return new MessageDispatchResult(
                delivery.senderUserId(),
                delivery.receiverUserId(),
                senderConversation,
                receiverConversation,
                senderMessage,
                receiverMessage);
    }

    public List<MessageNotificationDto> getNotifications(Long userId, Integer limit) {
        int normalizedLimit = normalizeNotificationLimit(limit);
        return messageNotificationRepository.findNotifications(userId, normalizedLimit);
    }

    public int getNotificationUnreadCount(Long userId) {
        return messageNotificationRepository.countUnread(userId);
    }

    public void markNotificationRead(Long userId, Long notificationId) {
        if (notificationId == null || notificationId <= 0) {
            throw new BusinessException("通知参数无效");
        }
        messageNotificationRepository.markNotificationRead(userId, notificationId, LocalDateTime.now());
    }

    public void markAllNotificationsRead(Long userId) {
        messageNotificationRepository.markAllRead(userId, LocalDateTime.now());
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

    private int normalizeNotificationLimit(Integer limit) {
        int resolved = limit == null ? 20 : limit;
        if (resolved < 1 || resolved > 50) {
            throw new BusinessException("通知数量限制必须在 1 到 50 之间");
        }
        return resolved;
    }

    public record MessageDispatchResult(
            Long senderUserId,
            Long receiverUserId,
            MessageConversationDto senderConversation,
            MessageConversationDto receiverConversation,
            MessageItemDto senderMessage,
            MessageItemDto receiverMessage) {
    }
}
