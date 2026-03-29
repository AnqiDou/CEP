package com.example.cep_backend.admin.dto;

import java.math.BigDecimal;

public record AdminItemDto(
        Long id,
        String title,
        String category,
        BigDecimal price,
        String owner,
        String status) {
}
