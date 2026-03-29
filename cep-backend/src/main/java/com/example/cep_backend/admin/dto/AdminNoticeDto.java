package com.example.cep_backend.admin.dto;

import java.time.LocalDateTime;

public record AdminNoticeDto(
        Long id,
        String content,
        LocalDateTime createdAt) {
}
