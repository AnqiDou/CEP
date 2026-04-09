package com.example.cep_backend.payment.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeOrderRecord(
                Long id,
                String orderNo,
                Long itemId,
                Long buyerUserId,
                Long sellerUserId,
                String itemTitle,
                BigDecimal amount,
                String coverPhotoUrl,
                String status,
                String receiverName,
                String receiverPhone,
                String receiverAddress,
                Boolean buyerConfirmed,
                Boolean sellerConfirmed,
                String refundStatus,
                String refundType,
                LocalDateTime createdAt,
                LocalDateTime paidAt,
                LocalDateTime completedAt) {
}
