package com.example.cep_backend.profile.dto;

public record ProfileUpdateRequest(
        String username,
        String password,
        String avatar) {
}
