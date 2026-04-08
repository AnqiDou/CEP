package com.example.cep_backend.profile.dto;

public record ProfileFollowUserDto(
        Long userId,
        String username,
        String avatar,
        String followedAt) {
}
