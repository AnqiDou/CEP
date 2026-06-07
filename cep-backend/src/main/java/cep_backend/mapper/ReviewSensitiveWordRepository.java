package cep_backend.mapper;

import cep_backend.dto.AdminSensitiveWordDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ReviewSensitiveWordRepository {

    private final JdbcTemplate jdbcTemplate;

    public ReviewSensitiveWordRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> findEnabledWords() {
        String sql = """
                SELECT word
                FROM review_sensitive_words
                WHERE enabled = 1
                ORDER BY id ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("word"));
    }

    public List<AdminSensitiveWordDto> listAllWords() {
        String sql = """
                SELECT id, category, word, enabled, created_at, updated_at
                FROM review_sensitive_words
                ORDER BY id DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AdminSensitiveWordDto(
                rs.getLong("id"),
                rs.getString("category"),
                rs.getString("word"),
                rs.getBoolean("enabled"),
                formatDateTime(rs.getTimestamp("created_at")),
                formatDateTime(rs.getTimestamp("updated_at"))));
    }

    public int createWord(String category, String word, boolean enabled, LocalDateTime now) {
        String sql = """
                INSERT INTO review_sensitive_words (category, word, enabled, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        return jdbcTemplate.update(sql, category, word, enabled ? 1 : 0, Timestamp.valueOf(now),
                Timestamp.valueOf(now));
    }

    public int updateWord(Long id, String category, String word, boolean enabled, LocalDateTime now) {
        String sql = """
                UPDATE review_sensitive_words
                SET category = ?,
                    word = ?,
                    enabled = ?,
                    updated_at = ?
                WHERE id = ?
                """;
        return jdbcTemplate.update(sql, category, word, enabled ? 1 : 0, Timestamp.valueOf(now), id);
    }

    public int deleteWord(Long id) {
        String sql = "DELETE FROM review_sensitive_words WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }

    private String formatDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime().toString();
    }
}
