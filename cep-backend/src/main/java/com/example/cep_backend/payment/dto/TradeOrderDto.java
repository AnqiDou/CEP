package com.example.cep_backend.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeOrderDto(
        Long id,
        String orderNo,
        Long itemId,
        String itemTitle,
        BigDecimal amount,
        String coverPhotoUrl,
        String status,
        String receiverName,
        String receiverPhone,
        String receiverAddress,
        LocalDateTime createdAt,
        LocalDateTime paidAt) {
}
