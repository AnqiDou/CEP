package cep_backend.dto;
import java.util.List;

public record AdminSupportConversationDto(
                Long id,
                String title,
                String reportType,
                String reporterName,
                Long itemId,
                String itemTitle,
                String reportContent,
                String status,
                String preview,
                List<AdminSupportMessageDto> messages) {
}
