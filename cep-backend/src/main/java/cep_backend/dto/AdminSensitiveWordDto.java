package cep_backend.dto;

public record AdminSensitiveWordDto(
        Long id,
        String category,
        String word,
        Boolean enabled,
        String createdAt,
        String updatedAt) {
}
