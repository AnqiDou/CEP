package cep_backend.mapper;
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
                    rs.getString("quantity_mode"),
                    rs.getObject("total_quantity", Integer.class),
                    rs.getObject("sold_quantity", Integer.class),
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
            String quantityMode,
            Integer totalQuantity,
            Integer soldQuantity,
            String description,
            LocalDateTime now) {
        String sql = """
                INSERT INTO items (
                    category_id,
                    publisher_user_id,
                    title,
                    description,
                    price,
                    quantity_mode,
                    total_quantity,
                    sold_quantity,
                    badge,
                    status,
                    created_at,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PUBLISHED', ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, categoryId);
            statement.setLong(2, publisherUserId);
            statement.setString(3, itemName);
            statement.setString(4, description);
            statement.setBigDecimal(5, price);
            statement.setString(6, quantityMode);
            statement.setObject(7, totalQuantity);
            statement.setObject(8, soldQuantity);
            statement.setString(9, null);
            statement.setTimestamp(10, Timestamp.valueOf(now));
            statement.setTimestamp(11, Timestamp.valueOf(now));
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
                    i.quantity_mode,
                    i.total_quantity,
                    i.sold_quantity,
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
                    i.quantity_mode,
                    i.total_quantity,
                    i.sold_quantity,
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
            String quantityMode,
            Integer totalQuantity,
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
                    quantity_mode = ?,
                    total_quantity = ?,
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
                quantityMode,
                totalQuantity,
                Timestamp.valueOf(now),
                itemId);

        String upsertDetailSql = """
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
                ON DUPLICATE KEY UPDATE
                    publisher_user_id = VALUES(publisher_user_id),
                    purchase_date = VALUES(purchase_date),
                    usage_duration = VALUES(usage_duration),
                    updated_at = VALUES(updated_at)
                """;
        Timestamp timestampNow = Timestamp.valueOf(now);
        jdbcTemplate.update(
                upsertDetailSql,
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
                UPDATE items i
                LEFT JOIN item_details d ON d.item_id = i.id
                SET i.status = 'DELETED',
                    i.updated_at = ?
                WHERE COALESCE(i.publisher_user_id, d.publisher_user_id) = ?
                  AND i.id = ?
                  AND i.status <> 'DELETED'
                """;
        return jdbcTemplate.update(sql, Timestamp.valueOf(now), userId, itemId);
    }

    public int updateStatus(Long userId, Long itemId, String status, LocalDateTime now) {
        String sql = """
                UPDATE items i
                LEFT JOIN item_details d ON d.item_id = i.id
                SET i.status = ?,
                    i.updated_at = ?
                WHERE COALESCE(i.publisher_user_id, d.publisher_user_id) = ?
                  AND i.id = ?
                  AND i.status <> 'DELETED'
                """;
        return jdbcTemplate.update(sql, status, Timestamp.valueOf(now), userId, itemId);
    }

    public BigDecimal findPublishedAveragePriceByCategoryId(Long categoryId, Long excludeItemId) {
        String sql = """
                SELECT AVG(i.price)
                FROM items i
                WHERE i.status = 'PUBLISHED'
                  AND i.category_id = ?
                  AND (? IS NULL OR i.id <> ?)
                """;
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, categoryId, excludeItemId, excludeItemId);
    }

    public void replaceItemOpsColumns(Long itemId, List<String> columnCodes, LocalDateTime now) {
        jdbcTemplate.update("DELETE FROM item_ops_columns WHERE item_id = ?", itemId);
        if (columnCodes == null || columnCodes.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO item_ops_columns (item_id, column_code, created_at, updated_at)
                VALUES (?, ?, ?, ?)
                """;
        Timestamp ts = Timestamp.valueOf(now);
        for (String columnCode : columnCodes) {
            jdbcTemplate.update(sql, itemId, columnCode, ts, ts);
        }
    }

    public record PublishOwnedItemBaseRecord(
            Long id,
            String name,
            String categoryCode,
            BigDecimal price,
            String quantityMode,
            Integer totalQuantity,
            Integer soldQuantity,
            LocalDate purchaseDate,
            String usageDuration,
            String description,
            String status,
            LocalDateTime createdAt) {

        public Integer remainingQuantity() {
            if ("UNLIMITED".equals(quantityMode)) {
                return null;
            }
            int total = totalQuantity == null ? 1 : totalQuantity;
            int sold = soldQuantity == null ? 0 : soldQuantity;
            return Math.max(total - sold, 0);
        }
    }
}
