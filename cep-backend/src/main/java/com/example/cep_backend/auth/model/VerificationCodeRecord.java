package com.example.cep_backend.auth.model;

import java.time.LocalDateTime;

public record VerificationCodeRecord(Long id, String code, LocalDateTime expiresAt, boolean used) {
}
