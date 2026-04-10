package cep_backend.mapper;
import cep_backend.entity.po.UserRecord;
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
            rs.getString("password_hash"),
            rs.getString("status"));

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserRecord> findByEmail(String email) {
        String sql = "SELECT id, email, username, password_hash, status FROM users WHERE email = ?";
        List<UserRecord> result = jdbcTemplate.query(sql, userRowMapper, email);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.getFirst());
    }

    public Optional<UserRecord> findById(Long userId) {
        String sql = "SELECT id, email, username, password_hash, status FROM users WHERE id = ?";
        List<UserRecord> result = jdbcTemplate.query(sql, userRowMapper, userId);
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

    public void updatePasswordByEmail(String email, String passwordHash, LocalDateTime now) {
        String sql = "UPDATE users SET password_hash = ?, updated_at = ? WHERE email = ?";
        jdbcTemplate.update(sql, passwordHash, now, email);
    }

    public void updateBasicInfo(Long userId, String username, LocalDateTime now) {
        String sql = "UPDATE users SET username = ?, updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, username, now, userId);
    }
}
