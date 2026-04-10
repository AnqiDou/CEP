package cep_backend.dto;
import java.math.BigDecimal;

public record AdminUserCreditScoreRequest(String role, BigDecimal creditScore) {
}
