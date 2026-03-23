package com.example.cep_backend.auth.service;

public interface EmailService {
    void sendRegisterCode(String targetEmail, String code);
}
