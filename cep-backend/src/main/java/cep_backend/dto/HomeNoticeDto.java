package cep_backend.dto;
import java.time.LocalDateTime;

public record HomeNoticeDto(
        Long id,
        String content,
        LocalDateTime createdAt) {
}
