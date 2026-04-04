package com.example.cep_backend.message.dto;

public record MessageNotificationDto(
        Long id,
        String type,
        String title,
        String content,
        Long relatedItemId,
        Long relatedUserId,
        boolean read,
        String createdAt) {
}
