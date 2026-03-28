package com.example.cep_backend.profile.dto;

import java.math.BigDecimal;

public record OtherProfileItemDto(
        Long id,
        Long itemId,
        String title,
        BigDecimal price,
        String status,
        String image,
        String time) {
}
