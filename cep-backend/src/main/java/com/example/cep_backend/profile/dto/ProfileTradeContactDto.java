package com.example.cep_backend.profile.dto;

public record ProfileTradeContactDto(
        Long orderId,
        Long itemId,
        Long peerUserId,
        String peerName,
        String itemTitle) {
}
