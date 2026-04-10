package cep_backend.dto;
import java.time.LocalDateTime;

public record AdminSupportMessageDto(
                Long id,
                String from,
                String content,
                String imageUrl,
                LocalDateTime createdAt) {
}
