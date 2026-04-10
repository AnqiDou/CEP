package cep_backend.dto;
import java.math.BigDecimal;

public record OtherProfileItemDto(
        Long id,
        Long itemId,
        String title,
        BigDecimal price,
        String status,
        String image,
        String time) {
}
