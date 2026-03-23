package com.example.cep_backend.auth.repository;

import com.example.cep_backend.auth.model.UserRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<UserRecord> userRowMapper = (rs, rowNum) -> new UserRecord(
            rs.getLong("id"),
            rs.getString("email"),
            rs.getString("username"),
            rs.getString("password_hash"));

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserRecord> findByEmail(String email) {
        String sql = "SELECT id, email, username, password_hash FROM users WHERE email = ?";
        List<UserRecord> result = jdbcTemplate.query(sql, userRowMapper, email);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.getFirst());
    }

    public long createUser(String email, String username, String passwordHash, LocalDateTime now) {
        String insertSql = """
                INSERT INTO users (email, username, password_hash, status, created_at, updated_at)
                VALUES (?, ?, ?, 'ACTIVE', ?, ?)
                """;
        jdbcTemplate.update(insertSql, email, username, passwordHash, now, now);

        String queryIdSql = "SELECT id FROM users WHERE email = ?";
        Long userId = jdbcTemplate.queryForObject(queryIdSql, Long.class, email);
        if (userId == null) {
            throw new IllegalStateException("用户创建失败");
        }
        return userId;
    }

    public void updateLastLoginAt(long userId, LocalDateTime loginAt) {
        String sql = "UPDATE users SET last_login_at = ?, updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, loginAt, loginAt, userId);
    }
}
