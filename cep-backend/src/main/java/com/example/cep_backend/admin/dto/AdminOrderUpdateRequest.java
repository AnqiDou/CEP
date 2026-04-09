package com.example.cep_backend.admin.dto;

public record AdminOrderUpdateRequest(
        String status,
        String refundStatus) {
}
