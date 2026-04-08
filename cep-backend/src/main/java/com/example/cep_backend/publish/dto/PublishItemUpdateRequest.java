package com.example.cep_backend.publish.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PublishItemUpdateRequest(
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
