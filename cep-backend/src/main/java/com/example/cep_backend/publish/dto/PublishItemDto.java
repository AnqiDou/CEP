package com.example.cep_backend.publish.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

public record PublishItemDto(
        Long id,
        String name,
        String categoryCode,
        BigDecimal price,
        String quantityMode,
        Integer totalQuantity,
        Integer soldQuantity,
        Integer remainingQuantity,
        LocalDate purchaseDate,
        String usageDuration,
        String description,
        List<String> photoUrls,
        LocalDateTime createdAt) {
}
