package cep_backend.dto;

public record ProfileTradeContactDto(
        Long orderId,
        Long itemId,
        Long peerUserId,
        String peerName,
        String itemTitle) {
}
