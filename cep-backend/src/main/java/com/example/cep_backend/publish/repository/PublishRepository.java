package com.example.cep_backend.publish.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public class PublishRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<PublishOwnedItemBaseRecord> ownedItemRowMapper = (rs,
            rowNum) -> new PublishOwnedItemBaseRecord(
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getString("category_code"),
                    rs.getBigDecimal("price"),
                    rs.getDate("purchase_date") == null ? null : rs.getDate("purchase_date").toLocalDate(),
                    rs.getString("usage_duration"),
                    rs.getString("description"),
                    rs.getString("status"),
                    rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime());

    public PublishRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long findCategoryIdByCode(String categoryCode) {
        String sql = "SELECT id FROM item_categories WHERE code = ?";
        List<Long> ids = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("id"), categoryCode);
        return ids.isEmpty() ? null : ids.getFirst();
    }

    public Long insertItem(Long categoryId,
            Long publisherUserId,
            String itemName,
            BigDecimal price,
            String description,
            LocalDateTime now) {
        String sql = """
                INSERT INTO items (
                    category_id,
                    publisher_user_id,
                    title,
                    description,
                    price,
                    campus,
                    badge,
                    status,
                    created_at,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'PUBLISHED', ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, categoryId);
            statement.setLong(2, publisherUserId);
            statement.setString(3, itemName);
            statement.setString(4, description);
            statement.setBigDecimal(5, price);
            statement.setString(6, "");
            statement.setString(7, null);
            statement.setTimestamp(8, Timestamp.valueOf(now));
            statement.setTimestamp(9, Timestamp.valueOf(now));
            return statement;
        }, keyHolder);

        if (keyHolder.getKey() == null) {
            throw new IllegalStateException("failed to create item");
        }
        return keyHolder.getKey().longValue();
    }

    public void insertItemPhotos(Long itemId, List<String> photoUrls, LocalDateTime now) {
        String sql = """
                INSERT INTO item_photos (item_id, photo_url, sort_order, created_at)
                VALUES (?, ?, ?, ?)
                """;
        for (int index = 0; index < photoUrls.size(); index++) {
            jdbcTemplate.update(sql, itemId, photoUrls.get(index), index + 1, Timestamp.valueOf(now));
        }
    }

    public void insertItemDetail(Long itemId,
            Long userId,
            LocalDate purchaseDate,
            String usageDuration,
            LocalDateTime now) {
        String sql = """
                INSERT INTO item_details (
                    item_id,
                    publisher_user_id,
                    purchase_date,
                    usage_duration,
                    item_condition,
                    accessories,
                    detail_note,
                    trade_location,
                    original_price,
                    created_at,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(
                sql,
                itemId,
                userId,
                purchaseDate,
                usageDuration,
                "",
                "",
                "",
                "",
                null,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now));
    }

    public List<PublishOwnedItemBaseRecord> findMyItems(Long userId) {
        String sql = """
                SELECT
                    i.id,
                    i.title,
                    c.code AS category_code,
                    i.price,
                    d.purchase_date,
                    d.usage_duration,
                    i.description,
                    i.status,
                    i.created_at
                FROM items i
                LEFT JOIN item_details d ON d.item_id = i.id
                INNER JOIN item_categories c ON c.id = i.category_id
                WHERE COALESCE(i.publisher_user_id, d.publisher_user_id) = ?
                  AND i.status <> 'DELETED'
                ORDER BY i.created_at DESC, i.id DESC
                """;
        return jdbcTemplate.query(sql, ownedItemRowMapper, userId);
    }

    public Optional<PublishOwnedItemBaseRecord> findOwnedItem(Long userId, Long itemId) {
        String sql = """
                SELECT
                    i.id,
                    i.title,
                    c.code AS category_code,
                    i.price,
                    d.purchase_date,
                    d.usage_duration,
                    i.description,
                    i.status,
                    i.created_at
                FROM items i
                LEFT JOIN item_details d ON d.item_id = i.id
                INNER JOIN item_categories c ON c.id = i.category_id
                WHERE COALESCE(i.publisher_user_id, d.publisher_user_id) = ?
                  AND i.id = ?
                """;
        List<PublishOwnedItemBaseRecord> records = jdbcTemplate.query(sql, ownedItemRowMapper, userId, itemId);
        if (records.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(records.getFirst());
    }

    public List<String> findPhotoUrlsByItemId(Long itemId) {
        String sql = """
                SELECT photo_url
                FROM item_photos
                WHERE item_id = ?
                ORDER BY sort_order ASC, id ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("photo_url"), itemId);
    }

    public void updateItemAndDetail(
            Long userId,
            Long itemId,
            Long categoryId,
            String itemName,
            BigDecimal price,
            String description,
            LocalDate purchaseDate,
            String usageDuration,
            LocalDateTime now) {
        String updateItemSql = """
                UPDATE items
                SET category_id = ?,
                    publisher_user_id = ?,
                    title = ?,
                    description = ?,
                    price = ?,
                    updated_at = ?
                WHERE id = ?
                """;
        jdbcTemplate.update(
                updateItemSql,
                categoryId,
                userId,
                itemName,
                description,
                price,
                Timestamp.valueOf(now),
                itemId);

        String upsertDetailSql = """
                UPDATE item_details
                SET publisher_user_id = ?,
                    purchase_date = ?,
                    usage_duration = ?,
                    updated_at = ?
                WHERE item_id = ?;

                IF @@ROWCOUNT = 0
                BEGIN
                    INSERT INTO item_details (
                        item_id,
                        publisher_user_id,
                        purchase_date,
                        usage_duration,
                        item_condition,
                        accessories,
                        detail_note,
                        trade_location,
                        original_price,
                        created_at,
                        updated_at
                    ) VALUES (?, ?, ?, ?, '', '', '', '', NULL, ?, ?)
                END
                """;
        Timestamp timestampNow = Timestamp.valueOf(now);
        jdbcTemplate.update(
                upsertDetailSql,
                userId,
                purchaseDate,
                usageDuration,
                timestampNow,
                itemId,
                itemId,
                userId,
                purchaseDate,
                usageDuration,
                timestampNow,
                timestampNow);
    }

    public void replaceItemPhotos(Long itemId, List<String> photoUrls, LocalDateTime now) {
        String deleteSql = "DELETE FROM item_photos WHERE item_id = ?";
        jdbcTemplate.update(deleteSql, itemId);
        insertItemPhotos(itemId, photoUrls, now);
    }

    public int markDeleted(Long userId, Long itemId, LocalDateTime now) {
        String sql = """
                UPDATE i
                SET i.status = 'DELETED',
                    i.updated_at = ?
                FROM items i
                LEFT JOIN item_details d ON d.item_id = i.id
                WHERE COALESCE(i.publisher_user_id, d.publisher_user_id) = ?
                  AND i.id = ?
                  AND i.status <> 'DELETED'
                """;
        return jdbcTemplate.update(sql, Timestamp.valueOf(now), userId, itemId);
    }

    public int updateStatus(Long userId, Long itemId, String status, LocalDateTime now) {
        String sql = """
                UPDATE i
                SET i.status = ?,
                    i.updated_at = ?
                FROM items i
                LEFT JOIN item_details d ON d.item_id = i.id
                WHERE COALESCE(i.publisher_user_id, d.publisher_user_id) = ?
                  AND i.id = ?
                  AND i.status <> 'DELETED'
                """;
        return jdbcTemplate.update(sql, status, Timestamp.valueOf(now), userId, itemId);
    }

    public record PublishOwnedItemBaseRecord(
            Long id,
            String name,
            String categoryCode,
            BigDecimal price,
            LocalDate purchaseDate,
            String usageDuration,
            String description,
            String status,
            LocalDateTime createdAt) {
    }
}
