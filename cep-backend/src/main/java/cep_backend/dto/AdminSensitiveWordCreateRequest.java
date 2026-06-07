package cep_backend.dto;

public record AdminSensitiveWordCreateRequest(
        String category,
        String word,
        Boolean enabled) {
}
