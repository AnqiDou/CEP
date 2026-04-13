package cep_backend.entity.po;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HomeItemRecord(Long id,
                Long categoryId,
                Long publisherUserId,
                Boolean self,
                String sellerName,
                String sellerAvatarUrl,
                BigDecimal sellerCreditScore,
                String categoryCode,
                String categoryName,
                String title,
                String description,
                BigDecimal price,
                String opsColumns,
                String photoUrl,
                LocalDateTime createdAt) {
}
