package cep_backend.dto;

public record AdminOrderUpdateRequest(
        String status,
        String refundStatus) {
}
