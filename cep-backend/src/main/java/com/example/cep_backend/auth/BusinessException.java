package com.example.cep_backend.auth;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
