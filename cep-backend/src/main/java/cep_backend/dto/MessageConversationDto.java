package cep_backend.dto;

public record MessageConversationDto(
                Long conversationId,
                Long peerUserId,
                String peerName,
                String peerAvatar,
                Long itemId,
                String itemTitle,
                String itemImage,
                boolean viewerIsBuyer,
                int unread,
                String lastMessage,
                String lastTime) {
}
