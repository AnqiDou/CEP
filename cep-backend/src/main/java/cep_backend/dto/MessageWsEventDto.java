package cep_backend.dto;

public record MessageWsEventDto(
        String eventType,
        MessageConversationDto conversation,
        MessageItemDto message) {
}
