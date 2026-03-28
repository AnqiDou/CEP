package com.example.cep_backend.itemdetail.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ItemDetailDto(
        Long id,
        Long categoryId,
        String categoryCode,
        String categoryName,
        String title,
        BigDecimal price,
        BigDecimal originalPrice,
        LocalDate purchaseDate,
        String usageDuration,
        String location,
        LocalDateTime publishTime,
        String condition,
        String accessories,
        String description,
        String detailNote,
        List<String> photos,
        ItemDetailPublisherDto publisher) {
}
