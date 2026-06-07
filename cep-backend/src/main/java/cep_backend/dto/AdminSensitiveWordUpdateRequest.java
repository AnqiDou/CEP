package cep_backend.dto;

public record AdminSensitiveWordUpdateRequest(
        String category,
        String word,
        Boolean enabled) {
}
