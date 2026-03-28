package com.example.cep_backend.publish.repository;

import org.springframework.jdbc.core.JdbcTemplate;
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

@Repository
public class PublishRepository {
    private final JdbcTemplate jdbcTemplate;

    public PublishRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long findCategoryIdByCode(String categoryCode) {
        String sql = "SELECT id FROM item_categories WHERE code = ?";
        List<Long> ids = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("id"), categoryCode);
        return ids.isEmpty() ? null : ids.getFirst();
    }

    public Long insertItem(Long categoryId,
            String itemName,
            BigDecimal price,
            String description,
            LocalDateTime now) {
        String sql = """
                INSERT INTO items (
                    category_id,
                    title,
                    description,
                    price,
                    campus,
                    badge,
                    status,
                    created_at,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, 'PUBLISHED', ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, categoryId);
            statement.setString(2, itemName);
            statement.setString(3, description);
            statement.setBigDecimal(4, price);
            statement.setString(5, "");
            statement.setString(6, null);
            statement.setTimestamp(7, Timestamp.valueOf(now));
            statement.setTimestamp(8, Timestamp.valueOf(now));
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
}
