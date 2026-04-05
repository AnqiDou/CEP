package com.example.cep_backend.home.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HomeItemDto(Long id,
                Long categoryId,
                Long publisherUserId,
                Boolean isSelf,
                String sellerName,
                String sellerAvatarUrl,
                String sellerCredit,
                String categoryCode,
                String categoryName,
                String title,
                String description,
                BigDecimal price,
                String badge,
                String opsColumns,
                String photoUrl,
                LocalDateTime createdAt) {
}
