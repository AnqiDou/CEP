package com.example.cep_backend.itemdetail.model;

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
        String campus,
        LocalDateTime createdAt,
        LocalDate purchaseDate,
        String usageDuration,
        String itemCondition,
        String accessories,
        String detailNote,
        String tradeLocation,
        BigDecimal originalPrice,
        Long publisherId,
        String publisherName,
        String publisherCollege,
        String publisherCampus,
        BigDecimal publisherCredit,
        String publisherNote) {
}
