package cep_backend.dto;
import java.math.BigDecimal;

public record ProfileOverviewDto(
        String avatar,
        String username,
        String name,
        String phone,
        String address,
        long fans,
        long following,
        BigDecimal sellerCreditScore,
        BigDecimal buyerCreditScore,
        String sellerCredit,
        String buyerCredit) {
}
