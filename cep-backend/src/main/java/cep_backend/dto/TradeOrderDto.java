package cep_backend.dto;
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
                Boolean buyerConfirmed,
                Boolean sellerConfirmed,
                String refundStatus,
                String refundType,
                LocalDateTime createdAt,
                LocalDateTime paidAt) {
}
