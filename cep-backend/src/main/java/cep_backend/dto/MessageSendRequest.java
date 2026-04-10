package cep_backend.dto;

public record MessageSendRequest(
        Long conversationId,
        String text,
        String imageUrl) {
}
