package cep_backend.dto;

public record ProfilePendingTradeDto(
        Long id,
        Long orderId,
        Long itemId,
        String title,
        String partner,
        String location,
        String time,
        String status,
        String statusText) {
}
