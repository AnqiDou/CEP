package com.example.cep_backend.auth.dto;

public record ResetPasswordRequest(String email, String code, String password) {
}
