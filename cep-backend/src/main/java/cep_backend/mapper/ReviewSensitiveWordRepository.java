package cep_backend.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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
}
