package com.example.cep_backend.auth.dto;

public record AuthSessionDto(
        Long userId,
        String email,
        String username,
        String accessToken,
        String refreshToken,
        long accessTokenExpiresInSeconds,
        String tokenType) {
}
