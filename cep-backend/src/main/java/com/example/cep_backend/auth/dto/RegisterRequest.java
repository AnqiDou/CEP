package com.example.cep_backend.auth.dto;

public record RegisterRequest(String email, String code, String username, String password) {
}
