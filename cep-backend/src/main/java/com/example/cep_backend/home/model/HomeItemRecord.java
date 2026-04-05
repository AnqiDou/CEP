package com.example.cep_backend.home.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HomeItemRecord(Long id,
                Long categoryId,
                Long publisherUserId,
                Boolean self,
                String sellerName,
                String sellerAvatarUrl,
                Integer sellerGoodCount,
                Integer sellerBadCount,
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
