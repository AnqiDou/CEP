package com.example.cep_backend.auth.model;

import java.time.LocalDateTime;

public record AuthSessionRecord(
        long id,
        long userId,
        String email,
        String username,
        LocalDateTime refreshExpiresAt,
        LocalDateTime accessExpiresAt,
        boolean revoked) {
}
