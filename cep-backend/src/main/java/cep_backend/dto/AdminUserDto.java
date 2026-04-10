package cep_backend.dto;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public record AdminUserDto(
        Long id,
        String name,
        String phone,
        String email,
        LocalDateTime registeredAt,
        BigDecimal sellerCreditScore,
        BigDecimal buyerCreditScore,
        Boolean disabled,
        Integer itemCount,
        Integer orderCount) {
}
