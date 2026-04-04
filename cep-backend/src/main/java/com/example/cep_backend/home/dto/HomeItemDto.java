package com.example.cep_backend.home.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HomeItemDto(Long id,
        Long categoryId,
        String categoryCode,
        String categoryName,
        String title,
        String description,
        BigDecimal price,
        String campus,
        String badge,
        String opsColumns,
        String photoUrl,
        LocalDateTime createdAt) {
}
