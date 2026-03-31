package com.example.cep_backend.message.dto;

public record MessageSendRequest(
        Long conversationId,
        String text,
        String imageUrl) {
}
