package com.example.cep_backend.message.dto;

public record MessageConversationDto(
        Long conversationId,
        Long peerUserId,
        String peerName,
        String peerAvatar,
        Long itemId,
        String itemTitle,
        String itemImage,
        int unread,
        String lastMessage,
        String lastTime) {
}
