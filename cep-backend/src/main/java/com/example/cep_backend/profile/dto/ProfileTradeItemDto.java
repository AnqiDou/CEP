package com.example.cep_backend.profile.dto;

import java.math.BigDecimal;

public record ProfileTradeItemDto(
        Long id,
        Long itemId,
        String title,
        BigDecimal price,
        String campus,
        String time,
        String photoUrl) {
}
