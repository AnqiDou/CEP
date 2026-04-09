package com.example.cep_backend.profile.dto;

import java.math.BigDecimal;

public record ProfileTradeItemDto(
                Long id,
                String orderNo,
                Long itemId,
                String title,
                BigDecimal price,
                String time,
                String photoUrl,
                String status,
                Boolean buyerConfirmed,
                Boolean sellerConfirmed,
                String refundStatus,
                String refundType) {
}
