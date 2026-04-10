package cep_backend.dto;

public record AuthSessionDto(
        Long userId,
        String email,
        String username,
        String accessToken,
        String refreshToken,
        long accessTokenExpiresInSeconds,
        String tokenType) {
}
