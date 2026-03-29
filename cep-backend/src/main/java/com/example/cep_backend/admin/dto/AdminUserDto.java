package com.example.cep_backend.admin.dto;

import java.time.LocalDateTime;

public record AdminUserDto(
        Long id,
        String name,
        String phone,
        String email,
        LocalDateTime registeredAt,
        Boolean disabled,
        Integer itemCount,
        Integer orderCount) {
}
