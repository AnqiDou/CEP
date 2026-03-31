package com.example.cep_backend.message.dto;

public record MessageCreateConversationRequest(
        Long peerUserId,
        Long itemId) {
}
