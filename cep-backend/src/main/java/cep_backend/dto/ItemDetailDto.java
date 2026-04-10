package cep_backend.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ItemDetailDto(
                Long id,
                Long categoryId,
                String categoryCode,
                String categoryName,
                String title,
                BigDecimal price,
                LocalDate purchaseDate,
                String usageDuration,
                LocalDateTime publishTime,
                String description,
                List<String> photos,
                ItemDetailPublisherDto publisher) {
}
