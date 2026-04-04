package com.example.cep_backend.itemdetail.dto;

public record ItemReportCreateResultDto(
        Long conversationId,
        String status,
        String message) {
}
