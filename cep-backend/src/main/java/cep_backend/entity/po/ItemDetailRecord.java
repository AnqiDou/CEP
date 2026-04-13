package cep_backend.entity.po;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ItemDetailRecord(
        Long itemId,
        Long categoryId,
        String categoryCode,
        String categoryName,
        String title,
        String description,
        BigDecimal price,
        LocalDateTime createdAt,
        LocalDate purchaseDate,
        String usageDuration,
        Long publisherId,
        String publisherName,
        String publisherAvatar,
        Integer publisherGoodCount,
        Integer publisherBadCount) {
}
