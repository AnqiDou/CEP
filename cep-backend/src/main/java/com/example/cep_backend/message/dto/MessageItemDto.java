package com.example.cep_backend.message.dto;

public record MessageItemDto(
        Long id,
        String from,
        String text,
        String imageUrl,
        String time) {
}
