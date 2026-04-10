package cep_backend.entity.po;
import java.time.LocalDateTime;

public record VerificationCodeRecord(Long id, String code, LocalDateTime expiresAt, boolean used) {
}
