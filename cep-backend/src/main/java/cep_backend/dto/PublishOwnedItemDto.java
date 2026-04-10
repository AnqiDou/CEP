package cep_backend.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PublishOwnedItemDto(
                Long id,
                String name,
                String categoryCode,
                BigDecimal price,
                String quantityMode,
                Integer totalQuantity,
                Integer soldQuantity,
                Integer remainingQuantity,
                LocalDate purchaseDate,
                String usageDuration,
                String description,
                List<String> photoUrls,
                String status,
                LocalDateTime createdAt) {
}
