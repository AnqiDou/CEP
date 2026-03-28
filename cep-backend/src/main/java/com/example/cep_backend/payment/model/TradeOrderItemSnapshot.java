package com.example.cep_backend.payment.model;

import java.math.BigDecimal;

public record TradeOrderItemSnapshot(
        Long itemId,
        String itemTitle,
        BigDecimal price,
        String coverPhotoUrl) {
}
