package com.example.cep_backend.publish.dto;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

public record PublishItemRequest(
        String name,
        String categoryCode,
        BigDecimal price,
        String quantityMode,
        Integer totalQuantity,
        LocalDate purchaseDate,
        String usageDuration,
        String description,
        List<String> photoUrls) {
}
