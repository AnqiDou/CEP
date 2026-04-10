package cep_backend.dto;
import java.time.LocalDateTime;

public record AdminNoticeDto(
        Long id,
        String content,
        LocalDateTime createdAt) {
}
