package com.example.cep_backend.auth.repository;

import com.example.cep_backend.auth.model.VerificationCodeRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class VerificationCodeRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<VerificationCodeRecord> codeRowMapper = (rs, rowNum) -> new VerificationCodeRecord(
            rs.getLong("id"),
            rs.getString("code"),
            rs.getObject("expires_at", LocalDateTime.class),
            rs.getBoolean("used"));

    public VerificationCodeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveCode(String email, String purpose, String code, LocalDateTime expiresAt, LocalDateTime now) {
        String sql = """
                INSERT INTO email_verification_codes (email, purpose, code, used, expires_at, created_at)
                VALUES (?, ?, ?, 0, ?, ?)
                """;
        jdbcTemplate.update(sql, email, purpose, code, expiresAt, now);
    }

    public Optional<LocalDateTime> findLastCreateTime(String email, String purpose) {
        String sql = """
                SELECT TOP 1 created_at
                FROM email_verification_codes
                WHERE email = ? AND purpose = ?
                ORDER BY created_at DESC
                """;
        List<LocalDateTime> result = jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getObject("created_at", LocalDateTime.class),
                email,
                purpose);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.getFirst());
    }

    public Optional<VerificationCodeRecord> findLatestUnUsedCode(String email, String purpose, LocalDateTime now) {
        String sql = """
                SELECT TOP 1 id, code, used, expires_at
                FROM email_verification_codes
                WHERE email = ? AND purpose = ? AND used = 0 AND expires_at > ?
                ORDER BY created_at DESC
                """;

        List<VerificationCodeRecord> result = jdbcTemplate.query(sql, codeRowMapper, email, purpose, now);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.getFirst());
    }

    public void markUsed(long id) {
        String sql = "UPDATE email_verification_codes SET used = 1 WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}
