package cep_backend.dto;

public record ResetPasswordRequest(String email, String code, String password) {
}
