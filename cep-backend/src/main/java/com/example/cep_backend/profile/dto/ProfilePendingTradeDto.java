package com.example.cep_backend.profile.dto;

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
