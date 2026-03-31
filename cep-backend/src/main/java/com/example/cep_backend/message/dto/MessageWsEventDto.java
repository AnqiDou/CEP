package com.example.cep_backend.message.dto;

public record MessageWsEventDto(
        String eventType,
        MessageConversationDto conversation,
        MessageItemDto message) {
}
