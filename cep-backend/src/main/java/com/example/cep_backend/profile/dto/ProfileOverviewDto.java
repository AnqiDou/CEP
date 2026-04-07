package com.example.cep_backend.profile.dto;

import java.math.BigDecimal;

public record ProfileOverviewDto(
                String avatar,
                String username,
                long fans,
                long following,
                BigDecimal sellerCreditScore,
                BigDecimal buyerCreditScore,
                String sellerCredit,
                String buyerCredit) {
}
