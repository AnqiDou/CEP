package cep_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminOrderDto(
                String orderNo,
                String itemTitle,
                String buyer,
                String seller,
                BigDecimal amount,
                String status,
                String refundStatus,
                LocalDateTime createdAt,
                LocalDateTime paidAt,
                LocalDateTime pendingConfirmationAt,
                LocalDateTime refundAppliedAt,
                LocalDateTime cancelledAt,
                LocalDateTime completedAt,
                LocalDateTime updatedAt) {
}
