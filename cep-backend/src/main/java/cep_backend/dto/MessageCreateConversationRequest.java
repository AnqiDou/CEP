package cep_backend.dto;

public record MessageCreateConversationRequest(
        Long peerUserId,
        Long itemId) {
}
