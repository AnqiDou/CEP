package com.example.cep_backend.itemdetail.dto;

import java.math.BigDecimal;

public record ItemDetailPublisherDto(
        Long id,
        String name,
        String college,
        String campus,
        BigDecimal credit,
        String note) {
}
