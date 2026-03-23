package com.example.cep_backend.auth.dto;

public record VerifyCodeRequest(String email, String code) {
}
