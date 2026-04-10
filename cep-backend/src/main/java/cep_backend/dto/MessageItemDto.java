package cep_backend.dto;

public record MessageItemDto(
                Long id,
                String from,
                String text,
                String imageUrl,
                String time,
                String messageType,
                Long reviewOrderId,
                String reviewStatus) {
}
