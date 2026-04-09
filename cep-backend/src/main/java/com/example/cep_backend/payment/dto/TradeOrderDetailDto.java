package com.example.cep_backend.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeOrderDetailDto(
        Long orderId,
        String orderNo,
        String status,
        String statusText,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime completedAt,
        Long itemId,
        String itemTitle,
        String itemDescription,
        String itemImage,
        BigDecimal amount,
        Long buyerUserId,
        String buyerName,
        String buyerAvatar,
        String buyerPhone,
        Long sellerUserId,
        String sellerName,
        String sellerAvatar,
        String sellerPhone) {
}
