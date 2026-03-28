package com.example.cep_backend.payment.dto;

public record CreateTradeOrderRequest(
        Long itemId,
        String receiverName,
        String receiverPhone,
        String receiverAddress) {
}
