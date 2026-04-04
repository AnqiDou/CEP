package com.example.cep_backend.itemdetail.repository;

import com.example.cep_backend.itemdetail.model.ItemDetailRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

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
            rs.getString("campus"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getDate("purchase_date") == null ? null : rs.getDate("purchase_date").toLocalDate(),
            rs.getString("usage_duration"),
            rs.getString("item_condition"),
            rs.getString("accessories"),
            rs.getString("detail_note"),
            rs.getString("trade_location"),
            rs.getBigDecimal("original_price"),
            rs.getObject("publisher_id", Long.class),
            rs.getString("publisher_name"),
            rs.getString("publisher_college"),
            rs.getString("publisher_campus"),
            rs.getBigDecimal("publisher_credit"),
            rs.getString("publisher_note"));

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
                    i.campus,
                    i.created_at,
                    d.purchase_date,
                    d.usage_duration,
                    d.item_condition,
                    d.accessories,
                    d.detail_note,
                    d.trade_location,
                    d.original_price,
                    u.id AS publisher_id,
                    COALESCE(NULLIF(u.username, ''), '校园用户') AS publisher_name,
                    up.college AS publisher_college,
                    up.campus AS publisher_campus,
                    up.credit_score AS publisher_credit,
                    up.note AS publisher_note
                FROM items i
                INNER JOIN item_categories c ON c.id = i.category_id
                LEFT JOIN item_details d ON d.item_id = i.id
                LEFT JOIN users u ON u.id = d.publisher_user_id
                LEFT JOIN user_profiles up ON up.user_id = u.id
                WHERE i.id = ? AND i.status = 'PUBLISHED'
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
}
