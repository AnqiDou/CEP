package cep_backend.dto;

public record ProfileFollowUserDto(
        Long userId,
        String username,
        String avatar,
        String followedAt) {
}
