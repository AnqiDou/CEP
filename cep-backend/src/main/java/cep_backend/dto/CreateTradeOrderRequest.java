package cep_backend.dto;

public record CreateTradeOrderRequest(
        Long itemId,
        String receiverName,
        String receiverPhone,
        String receiverAddress) {
}
