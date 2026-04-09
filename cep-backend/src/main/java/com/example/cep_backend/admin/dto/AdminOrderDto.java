package com.example.cep_backend.admin.dto;

import java.math.BigDecimal;

public record AdminOrderDto(
                String orderNo,
                String itemTitle,
                String buyer,
                String seller,
                BigDecimal amount,
                String status,
                String refundStatus) {
}
