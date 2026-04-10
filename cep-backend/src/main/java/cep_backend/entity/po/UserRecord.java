package cep_backend.entity.po;

public record UserRecord(Long id, String email, String username, String passwordHash, String status) {
}
