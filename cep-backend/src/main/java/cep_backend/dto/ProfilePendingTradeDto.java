package cep_backend.dto;

import java.math.BigDecimal;

public record ProfilePendingTradeDto(
        Long id,
        Long orderId,
        Long itemId,
        String title,
        BigDecimal price,
        String partner,
        String location,
        String time,
        String status,
        String statusText,
        Long cancelCountdownSeconds,
        String cancelDeadlineTime) {
}
