package cep_backend.mapper;
import cep_backend.dto.MessageNotificationDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Repository
public class MessageNotificationRepository {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final JdbcTemplate jdbcTemplate;

    public MessageNotificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MessageNotificationDto> findNotifications(Long userId, int limit) {
        String sql = """
                SELECT
                    n.id,
                    n.notification_type,
                    n.title,
                    n.content,
                    n.related_item_id,
                    n.related_user_id,
                    n.is_read,
                    n.created_at
                FROM message_notifications n
                WHERE n.user_id = ?
                ORDER BY n.created_at DESC, n.id DESC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp timestamp = rs.getTimestamp("created_at");
            LocalDateTime createdAt = timestamp == null ? LocalDateTime.now() : timestamp.toLocalDateTime();
            return new MessageNotificationDto(
                    rs.getLong("id"),
                    rs.getString("notification_type"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getObject("related_item_id", Long.class),
                    rs.getObject("related_user_id", Long.class),
                    rs.getBoolean("is_read"),
                    createdAt.format(DATE_TIME_FORMATTER));
        }, userId, limit);
    }

    public int countUnread(Long userId) {
        String sql = """
                SELECT COUNT(1)
                FROM message_notifications
                WHERE user_id = ? AND is_read = 0
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count == null ? 0 : count;
    }

    public void markNotificationRead(Long userId, Long notificationId, LocalDateTime now) {
        String sql = """
                UPDATE message_notifications
                SET is_read = 1,
                    read_at = ?,
                    updated_at = ?
                WHERE id = ?
                  AND user_id = ?
                """;
        Timestamp timestamp = Timestamp.valueOf(now);
        jdbcTemplate.update(sql, timestamp, timestamp, notificationId, userId);
    }

    public void markAllRead(Long userId, LocalDateTime now) {
        String sql = """
                UPDATE message_notifications
                SET is_read = 1,
                    read_at = CASE WHEN read_at IS NULL THEN ? ELSE read_at END,
                    updated_at = ?
                WHERE user_id = ?
                  AND is_read = 0
                """;
        Timestamp timestamp = Timestamp.valueOf(now);
        jdbcTemplate.update(sql, timestamp, timestamp, userId);
    }

    public void insertNotification(
            Long userId,
            String type,
            String title,
            String content,
            Long relatedItemId,
            Long relatedUserId,
            LocalDateTime now) {
        String sql = """
                INSERT INTO message_notifications (
                    user_id,
                    notification_type,
                    title,
                    content,
                    related_item_id,
                    related_user_id,
                    is_read,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?)
                """;
        Timestamp timestamp = Timestamp.valueOf(now);
        jdbcTemplate.update(sql,
                userId,
                type,
                title,
                content,
                relatedItemId,
                relatedUserId,
                timestamp,
                timestamp);
    }

    public void insertPriceDropNotifications(
            Long itemId,
            Long ownerUserId,
            String title,
            String content,
            LocalDateTime now) {
        String sql = """
                INSERT INTO message_notifications (
                    user_id,
                    notification_type,
                    title,
                    content,
                    related_item_id,
                    related_user_id,
                    is_read,
                    created_at,
                    updated_at
                )
                SELECT
                    f.user_id,
                    'FAVORITE_PRICE_DROP',
                    ?,
                    ?,
                    ?,
                    ?,
                    0,
                    ?,
                    ?
                FROM user_favorites f
                WHERE f.item_id = ?
                  AND f.user_id <> ?
                """;
        Timestamp timestamp = Timestamp.valueOf(now);
        jdbcTemplate.update(sql,
                title,
                content,
                itemId,
                ownerUserId,
                timestamp,
                timestamp,
                itemId,
                ownerUserId);
    }

    public void insertOffShelfNotifications(
            Long itemId,
            Long ownerUserId,
            String title,
            String content,
            LocalDateTime now) {
        String sql = """
                INSERT INTO message_notifications (
                    user_id,
                    notification_type,
                    title,
                    content,
                    related_item_id,
                    related_user_id,
                    is_read,
                    created_at,
                    updated_at
                )
                SELECT
                    f.user_id,
                    'FAVORITE_OFF_SHELF',
                    ?,
                    ?,
                    ?,
                    ?,
                    0,
                    ?,
                    ?
                FROM user_favorites f
                WHERE f.item_id = ?
                  AND f.user_id <> ?
                """;
        Timestamp timestamp = Timestamp.valueOf(now);
        jdbcTemplate.update(sql,
                title,
                content,
                itemId,
                ownerUserId,
                timestamp,
                timestamp,
                itemId,
                ownerUserId);
    }

    public String findUsernameByUserId(Long userId) {
        String sql = "SELECT COALESCE(NULLIF(username, ''), '校园用户') AS username FROM users WHERE id = ? LIMIT 1";
        List<String> list = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("username"), userId);
        return list.isEmpty() ? "校园用户" : list.getFirst();
    }

    public String findItemTitle(Long itemId) {
        String sql = "SELECT title FROM items WHERE id = ? LIMIT 1";
        List<String> list = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("title"), itemId);
        return list.isEmpty() ? "闲置物品" : list.getFirst();
    }

    public BigDecimal findItemPrice(Long itemId) {
        String sql = "SELECT price FROM items WHERE id = ? LIMIT 1";
        List<BigDecimal> list = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getBigDecimal("price"), itemId);
        return list.isEmpty() ? null : list.getFirst();
    }
}
