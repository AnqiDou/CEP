package com.example.cep_backend.admin.dto;

import java.math.BigDecimal;

public record AdminUserCreditScoreRequest(String role, BigDecimal creditScore) {
}
