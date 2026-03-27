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
                LocalDate purchaseDate,
                String usageDuration,
                String description,
                List<String> photoUrls,
                LocalDateTime createdAt) {
}
