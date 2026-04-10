package cep_backend.entity.po;
import java.time.LocalDateTime;

public record AuthSessionRecord(
        long id,
        long userId,
        String email,
        String username,
        LocalDateTime refreshExpiresAt,
        LocalDateTime accessExpiresAt,
        boolean revoked) {
}
