package com.example.cep_backend.auth.repository;

import com.example.cep_backend.auth.model.AuthSessionRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AuthSessionRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<AuthSessionRecord> rowMapper = (rs, rowNum) -> new AuthSessionRecord(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("email"),
            rs.getString("username"),
            rs.getObject("refresh_expires_at", LocalDateTime.class),
            rs.getObject("access_expires_at", LocalDateTime.class),
            rs.getBoolean("revoked"));

    public AuthSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void createSession(long userId,
            String refreshTokenHash,
            String accessTokenHash,
            LocalDateTime refreshExpiresAt,
            LocalDateTime accessExpiresAt,
            LocalDateTime now) {
        String sql = """
                INSERT INTO auth_sessions (user_id, refresh_token_hash, access_token_hash, refresh_expires_at, access_expires_at, revoked, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 0, ?, ?)
                """;
        jdbcTemplate.update(sql, userId, refreshTokenHash, accessTokenHash, refreshExpiresAt, accessExpiresAt, now,
                now);
    }

    public Optional<AuthSessionRecord> findByRefreshTokenHash(String refreshTokenHash) {
        String sql = """
                SELECT TOP 1 s.id, s.user_id, u.email, u.username, s.refresh_expires_at, s.access_expires_at, s.revoked
                FROM auth_sessions s
                INNER JOIN users u ON u.id = s.user_id
                WHERE s.refresh_token_hash = ?
                """;
        List<AuthSessionRecord> result = jdbcTemplate.query(sql, rowMapper, refreshTokenHash);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.getFirst());
    }

    public Optional<AuthSessionRecord> findByAccessTokenHash(String accessTokenHash) {
        String sql = """
                SELECT TOP 1 s.id, s.user_id, u.email, u.username, s.refresh_expires_at, s.access_expires_at, s.revoked
                FROM auth_sessions s
                INNER JOIN users u ON u.id = s.user_id
                WHERE s.access_token_hash = ?
                """;
        List<AuthSessionRecord> result = jdbcTemplate.query(sql, rowMapper, accessTokenHash);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.getFirst());
    }

    public void rotateSession(long sessionId,
            String newRefreshTokenHash,
            String newAccessTokenHash,
            LocalDateTime refreshExpiresAt,
            LocalDateTime accessExpiresAt,
            LocalDateTime now) {
        String sql = """
                UPDATE auth_sessions
                SET refresh_token_hash = ?,
                    access_token_hash = ?,
                    refresh_expires_at = ?,
                    access_expires_at = ?,
                    updated_at = ?
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, newRefreshTokenHash, newAccessTokenHash, refreshExpiresAt, accessExpiresAt, now,
                sessionId);
    }

    public void revokeSession(long sessionId, LocalDateTime now) {
        String sql = "UPDATE auth_sessions SET revoked = 1, updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, now, sessionId);
    }
}
