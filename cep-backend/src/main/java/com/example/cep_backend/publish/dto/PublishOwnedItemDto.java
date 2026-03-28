package com.example.cep_backend.publish.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PublishOwnedItemDto(
        Long id,
        String name,
        String categoryCode,
        BigDecimal price,
        LocalDate purchaseDate,
        String usageDuration,
        String description,
        List<String> photoUrls,
        String status,
        LocalDateTime createdAt) {
}
