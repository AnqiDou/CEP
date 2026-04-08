package com.example.cep_backend.auth.model;

public record UserRecord(Long id, String email, String username, String passwordHash, String status) {
}
