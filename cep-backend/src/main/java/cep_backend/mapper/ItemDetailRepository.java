package cep_backend.mapper;

import cep_backend.entity.po.ItemDetailRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ItemDetailRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<ItemDetailRecord> itemDetailRowMapper = (rs, rowNum) -> new ItemDetailRecord(
            rs.getLong("item_id"),
            rs.getLong("category_id"),
            rs.getString("category_code"),
            rs.getString("category_name"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getBigDecimal("price"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getDate("purchase_date") == null ? null : rs.getDate("purchase_date").toLocalDate(),
            rs.getString("usage_duration"),
            rs.getObject("publisher_id", Long.class),
            rs.getString("publisher_name"),
            rs.getString("publisher_avatar"),
            rs.getObject("publisher_good_count", Integer.class),
            rs.getObject("publisher_bad_count", Integer.class));

    public ItemDetailRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ItemDetailRecord findByItemId(Long itemId) {
        String sql = """
                SELECT
                    i.id AS item_id,
                    i.category_id,
                    c.code AS category_code,
                    c.name AS category_name,
                    i.title,
                    i.description,
                    i.price,
                    i.created_at,
                    d.purchase_date,
                    d.usage_duration,
                    u.id AS publisher_id,
                    COALESCE(NULLIF(u.username, ''), '校园用户') AS publisher_name,
                    up.avatar_url AS publisher_avatar,
                    (
                        SELECT COUNT(1)
                        FROM user_credit_reviews r
                        WHERE r.target_user_id = u.id
                          AND r.target_role = 'SELLER'
                          AND r.rating = 'good'
                    ) AS publisher_good_count,
                    (
                        SELECT COUNT(1)
                        FROM user_credit_reviews r
                        WHERE r.target_user_id = u.id
                          AND r.target_role = 'SELLER'
                          AND r.rating = 'bad'
                    ) AS publisher_bad_count
                FROM items i
                INNER JOIN item_categories c ON c.id = i.category_id
                LEFT JOIN item_details d ON d.item_id = i.id
                LEFT JOIN users u ON u.id = d.publisher_user_id
                LEFT JOIN user_profiles up ON up.user_id = u.id
                WHERE i.id = ?
                  AND (
                        i.status = 'PUBLISHED'
                        OR (
                            i.status = 'OFF_SHELF'
                            AND i.quantity_mode <> 'UNLIMITED'
                            AND COALESCE(i.sold_quantity, 0) >= COALESCE(i.total_quantity, 1)
                        )
                  )
                """;
        List<ItemDetailRecord> records = jdbcTemplate.query(sql, itemDetailRowMapper, itemId);
        return records.isEmpty() ? null : records.getFirst();
    }

    public List<String> findPhotoUrls(Long itemId) {
        String sql = """
                SELECT photo_url
                FROM item_photos
                WHERE item_id = ?
                ORDER BY sort_order ASC, id ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("photo_url"), itemId);
    }

    public void increaseViewCount(Long itemId) {
        String sql = "UPDATE items SET view_count = view_count + 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        jdbcTemplate.update(sql, itemId);
    }

    public boolean isFavorite(Long userId, Long itemId) {
        String sql = "SELECT COUNT(1) FROM user_favorites WHERE user_id = ? AND item_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, itemId);
        return count != null && count > 0;
    }

    public boolean addFavorite(Long userId, Long itemId, LocalDateTime now) {
        String insertSql = """
                INSERT INTO user_favorites (user_id, item_id, created_at)
                SELECT ?, ?, ?
                FROM DUAL
                WHERE NOT EXISTS (
                    SELECT 1 FROM user_favorites WHERE user_id = ? AND item_id = ?
                )
                """;
        int inserted = jdbcTemplate.update(insertSql, userId, itemId, Timestamp.valueOf(now), userId, itemId);
        if (inserted <= 0) {
            return false;
        }

        String updateFavoriteCountSql = """
                UPDATE items i
                SET i.favorite_count = (
                    SELECT COUNT(1) FROM user_favorites f WHERE f.item_id = i.id
                ),
                i.updated_at = ?
                WHERE i.id = ?
                """;
        jdbcTemplate.update(updateFavoriteCountSql, Timestamp.valueOf(now), itemId);
        return true;
    }

    public void removeFavorite(Long userId, Long itemId, LocalDateTime now) {
        String deleteSql = "DELETE FROM user_favorites WHERE user_id = ? AND item_id = ?";
        jdbcTemplate.update(deleteSql, userId, itemId);

        String updateFavoriteCountSql = """
                UPDATE items i
                SET i.favorite_count = (
                    SELECT COUNT(1) FROM user_favorites f WHERE f.item_id = i.id
                ),
                i.updated_at = ?
                WHERE i.id = ?
                """;
        jdbcTemplate.update(updateFavoriteCountSql, Timestamp.valueOf(now), itemId);
    }

    public Long findItemOwnerUserId(Long itemId) {
        String sql = """
                SELECT COALESCE(i.publisher_user_id, d.publisher_user_id) AS owner_user_id
                FROM items i
                LEFT JOIN item_details d ON d.item_id = i.id
                WHERE i.id = ?
                LIMIT 1
                """;
        List<Long> list = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getObject("owner_user_id", Long.class), itemId);
        return list.isEmpty() ? null : list.getFirst();
    }

    public ItemReportMeta findItemReportMetaByItemId(Long itemId) {
        String sql = """
                SELECT
                    i.id AS item_id,
                    i.title AS item_title,
                    COALESCE(i.publisher_user_id, d.publisher_user_id) AS publisher_user_id
                FROM items i
                LEFT JOIN item_details d ON d.item_id = i.id
                WHERE i.id = ?
                  AND i.status <> 'DELETED'
                LIMIT 1
                """;
        List<ItemReportMeta> list = jdbcTemplate.query(sql, (rs, rowNum) -> new ItemReportMeta(
                rs.getLong("item_id"),
                rs.getString("item_title"),
                rs.getObject("publisher_user_id", Long.class)), itemId);
        return list.isEmpty() ? null : list.getFirst();
    }

    public boolean existsOpenReportForItemAndReporter(Long itemId, Long reporterUserId) {
        String sql = """
                SELECT COUNT(1)
                FROM admin_support_conversations
                WHERE item_id = ?
                  AND reporter_user_id = ?
                  AND status IN ('OPEN', 'PROCESSING')
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, itemId, reporterUserId);
        return count != null && count > 0;
    }

    public Long createAdminSupportConversation(
            String title,
            String reportType,
            Long reporterUserId,
            Long itemId,
            String reportContent,
            String preview,
            String status,
            LocalDateTime now) {
        String sql = """
                INSERT INTO admin_support_conversations (
                    title,
                    report_type,
                    reporter_user_id,
                    item_id,
                    report_content,
                    preview,
                    status,
                    created_at,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, title);
            statement.setString(2, reportType);
            statement.setLong(3, reporterUserId);
            statement.setLong(4, itemId);
            statement.setString(5, reportContent);
            statement.setString(6, preview);
            statement.setString(7, status);
            statement.setTimestamp(8, Timestamp.valueOf(now));
            statement.setTimestamp(9, Timestamp.valueOf(now));
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            return null;
        }
        return key.longValue();
    }

    public int insertAdminSupportMessage(Long conversationId, String senderType, String content, LocalDateTime now) {
        String sql = """
                INSERT INTO admin_support_messages (
                    conversation_id,
                    sender_type,
                    content,
                    created_at
                ) VALUES (?, ?, ?, ?)
                """;
        return jdbcTemplate.update(sql, conversationId, senderType, content, Timestamp.valueOf(now));
    }

    public record ItemReportMeta(Long itemId, String itemTitle, Long publisherUserId) {
    }
}
