package com.example.cep_backend.payment.repository;

import com.example.cep_backend.payment.model.TradeOrderItemSnapshot;
import com.example.cep_backend.payment.model.TradeOrderRecord;
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
public class TradeOrderRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<TradeOrderItemSnapshot> itemSnapshotRowMapper = (rs, rowNum) -> new TradeOrderItemSnapshot(
            rs.getLong("item_id"),
            rs.getObject("seller_user_id", Long.class),
            rs.getString("item_title"),
            rs.getBigDecimal("price"),
            rs.getString("cover_photo_url"));

    private final RowMapper<TradeOrderRecord> tradeOrderRowMapper = (rs, rowNum) -> new TradeOrderRecord(
            rs.getLong("id"),
            rs.getString("order_no"),
            rs.getLong("item_id"),
            rs.getObject("buyer_user_id", Long.class),
            rs.getObject("seller_user_id", Long.class),
            rs.getString("item_title"),
            rs.getBigDecimal("amount"),
            rs.getString("cover_photo_url"),
            rs.getString("status"),
            rs.getString("receiver_name"),
            rs.getString("receiver_phone"),
            rs.getString("receiver_address"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("paid_at") == null ? null : rs.getTimestamp("paid_at").toLocalDateTime());

    public TradeOrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public TradeOrderItemSnapshot findPublishedItemSnapshot(Long itemId) {
        String sql = """
                SELECT
                    i.id AS item_id,
                    COALESCE(i.publisher_user_id, d.publisher_user_id) AS seller_user_id,
                    i.title AS item_title,
                    i.price,
                    (
                        SELECT p.photo_url
                        FROM item_photos p
                        WHERE p.item_id = i.id
                        ORDER BY p.sort_order ASC, p.id ASC
                        LIMIT 1
                    ) AS cover_photo_url
                FROM items i
                LEFT JOIN item_details d ON d.item_id = i.id
                WHERE i.id = ? AND i.status = 'PUBLISHED'
                """;

        List<TradeOrderItemSnapshot> snapshots = jdbcTemplate.query(sql, itemSnapshotRowMapper, itemId);
        return snapshots.isEmpty() ? null : snapshots.getFirst();
    }

    public Long createOrder(
            String orderNo,
            TradeOrderItemSnapshot snapshot,
            Long buyerUserId,
            String receiverName,
            String receiverPhone,
            String receiverAddress) {
        String sql = """
                INSERT INTO trade_orders (
                    order_no,
                    item_id,
                    buyer_user_id,
                    seller_user_id,
                    item_title,
                    amount,
                    cover_photo_url,
                    receiver_name,
                    receiver_phone,
                    receiver_address,
                    status,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING_PAYMENT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, orderNo);
            statement.setLong(2, snapshot.itemId());
            statement.setLong(3, buyerUserId);
            statement.setLong(4, snapshot.sellerUserId());
            statement.setString(5, snapshot.itemTitle());
            statement.setBigDecimal(6, snapshot.price());
            statement.setString(7, snapshot.coverPhotoUrl());
            statement.setString(8, receiverName);
            statement.setString(9, receiverPhone);
            statement.setString(10, receiverAddress);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            return null;
        }
        return key.longValue();
    }

    public TradeOrderRecord findOrderById(Long orderId) {
        String sql = """
                SELECT
                    id,
                    order_no,
                    item_id,
                    buyer_user_id,
                    seller_user_id,
                    item_title,
                    amount,
                    cover_photo_url,
                    status,
                    receiver_name,
                    receiver_phone,
                    receiver_address,
                    created_at,
                    paid_at
                FROM trade_orders
                WHERE id = ?
                """;

        List<TradeOrderRecord> records = jdbcTemplate.query(sql, tradeOrderRowMapper, orderId);
        return records.isEmpty() ? null : records.getFirst();
    }

    public int markOrderPaid(Long orderId, LocalDateTime paidAt) {
        String sql = """
                UPDATE trade_orders
                SET status = 'PAID',
                    paid_at = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND status = 'PENDING_PAYMENT'
                """;
        return jdbcTemplate.update(sql, Timestamp.valueOf(paidAt), orderId);
    }
}
