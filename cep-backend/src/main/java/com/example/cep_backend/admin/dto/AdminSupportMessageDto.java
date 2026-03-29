package com.example.cep_backend.admin.dto;

import java.time.LocalDateTime;

public record AdminSupportMessageDto(
        Long id,
        String from,
        String content,
        LocalDateTime createdAt) {
}
