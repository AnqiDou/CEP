package com.example.cep_backend.admin.dto;

import java.util.List;

public record AdminSupportConversationDto(
        Long id,
        String title,
        String preview,
        List<AdminSupportMessageDto> messages) {
}
