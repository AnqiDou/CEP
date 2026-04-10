package cep_backend.dto;

public record RegisterRequest(
        String email,
        String code,
        String username,
        String password,
        String name,
        String phone,
        String address) {
}
