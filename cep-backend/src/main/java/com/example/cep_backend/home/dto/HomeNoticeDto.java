package com.example.cep_backend.home.dto;

import java.time.LocalDateTime;

public record HomeNoticeDto(
        Long id,
        String content,
        LocalDateTime createdAt) {
}
