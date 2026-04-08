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
            rs.getString("cover_photo_url"),
            rs.getString("quantity_mode"),
            rs.getObject("total_quantity", Integer.class),
            rs.getObject("sold_quantity", Integer.class),
            rs.getObject("remaining_quantity", Integer.class));

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
            rs.getObject("buyer_confirmed", Boolean.class),
            rs.getObject("seller_confirmed", Boolean.class),
            rs.getString("refund_status"),
            rs.getString("refund_type"),
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
                    i.quantity_mode,
                    i.total_quantity,
                    i.sold_quantity,
                    CASE
                        WHEN i.quantity_mode = 'UNLIMITED' THEN NULL
                        ELSE GREATEST(COALESCE(i.total_quantity, 1) - COALESCE(i.sold_quantity, 0), 0)
                    END AS remaining_quantity,
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

    public boolean existsPendingOrderForItem(Long itemId) {
        String sql = """
                SELECT COUNT(1)
                FROM trade_orders
                WHERE item_id = ?
                  AND status = 'PENDING_PAYMENT'
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, itemId);
        return count != null && count > 0;
    }

    public TradeOrderRecord findLatestPendingOrderByBuyerAndItem(Long buyerUserId, Long itemId) {
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
                    buyer_confirmed,
                    seller_confirmed,
                    refund_status,
                    refund_type,
                    created_at,
                    paid_at
                FROM trade_orders
                WHERE buyer_user_id = ?
                  AND item_id = ?
                  AND status = 'PENDING_PAYMENT'
                ORDER BY created_at DESC, id DESC
                LIMIT 1
                """;
        List<TradeOrderRecord> records = jdbcTemplate.query(sql, tradeOrderRowMapper, buyerUserId, itemId);
        return records.isEmpty() ? null : records.getFirst();
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
                    buyer_confirmed,
                    seller_confirmed,
                    refund_status,
                    refund_type,
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
                SET status = 'PENDING_CONFIRMATION',
                    paid_at = ?,
                    buyer_confirmed = FALSE,
                    seller_confirmed = FALSE,
                    refund_status = 'NONE',
                    refund_type = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND status = 'PENDING_PAYMENT'
                """;
        return jdbcTemplate.update(sql, Timestamp.valueOf(paidAt), orderId);
    }

    public int markSellerConfirmedDelivery(Long orderId) {
        String sql = """
                UPDATE trade_orders
                SET seller_confirmed = TRUE,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND status = 'PENDING_CONFIRMATION'
                """;
        return jdbcTemplate.update(sql, orderId);
    }

    public int markBuyerConfirmedReceived(Long orderId) {
        String sql = """
                UPDATE trade_orders
                SET buyer_confirmed = TRUE,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND status = 'PENDING_CONFIRMATION'
                """;
        return jdbcTemplate.update(sql, orderId);
    }

    public int completeOrderWhenBothConfirmed(Long orderId) {
        String sql = """
                UPDATE trade_orders
                SET status = 'COMPLETED',
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND status = 'PENDING_CONFIRMATION'
                  AND buyer_confirmed = TRUE
                  AND seller_confirmed = TRUE
                """;
        return jdbcTemplate.update(sql, orderId);
    }

    public int applyRefund(Long orderId, String refundType) {
        String sql = """
                UPDATE trade_orders
                SET refund_status = 'APPLIED',
                    refund_type = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND status = 'PENDING_CONFIRMATION'
                  AND COALESCE(refund_status, 'NONE') = 'NONE'
                """;
        return jdbcTemplate.update(sql, refundType, orderId);
    }

    public int approveRefundNoReceipt(Long orderId) {
        String sql = """
                UPDATE trade_orders
                SET status = 'CANCELLED',
                    refund_status = 'APPROVED',
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND status = 'PENDING_CONFIRMATION'
                  AND refund_status = 'APPLIED'
                  AND refund_type = 'NO_RECEIPT'
                """;
        return jdbcTemplate.update(sql, orderId);
    }

    public int approveRefundAfterReturn(Long orderId) {
        String sql = """
                UPDATE trade_orders
                SET status = 'CANCELLED',
                    refund_status = 'APPROVED',
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND status = 'PENDING_CONFIRMATION'
                  AND refund_status = 'APPLIED'
                  AND refund_type = 'RETURN_AFTER_RECEIPT'
                """;
        return jdbcTemplate.update(sql, orderId);
    }

    public int cancelPendingOrder(Long orderId, Long actorUserId) {
        String sql = """
                UPDATE trade_orders
                SET status = 'CANCELLED',
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND status = 'PENDING_PAYMENT'
                  AND (buyer_user_id = ? OR seller_user_id = ?)
                """;
        return jdbcTemplate.update(sql, orderId, actorUserId, actorUserId);
    }

    public int cancelPendingOrdersByBuyerAndItem(Long buyerUserId, Long itemId) {
        String sql = """
                UPDATE trade_orders
                SET status = 'CANCELLED',
                    updated_at = CURRENT_TIMESTAMP
                WHERE buyer_user_id = ?
                  AND item_id = ?
                  AND status = 'PENDING_PAYMENT'
                """;
        return jdbcTemplate.update(sql, buyerUserId, itemId);
    }

    public boolean restoreOneStockOnRefund(Long itemId) {
        String sql = """
                UPDATE items
                SET sold_quantity = GREATEST(COALESCE(sold_quantity, 0) - 1, 0),
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;
        int updated = jdbcTemplate.update(sql, itemId);
        return updated > 0;
    }

    public boolean consumeOneStockOnPaid(Long itemId) {
        String sql = """
                UPDATE items
                SET sold_quantity = COALESCE(sold_quantity, 0) + 1,
                    status = CASE
                        WHEN quantity_mode = 'UNLIMITED' THEN status
                        WHEN (COALESCE(sold_quantity, 0) + 1) >= COALESCE(total_quantity, 1) THEN 'OFF_SHELF'
                        ELSE status
                    END,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND status = 'PUBLISHED'
                  AND (
                        quantity_mode = 'UNLIMITED'
                        OR COALESCE(sold_quantity, 0) < COALESCE(total_quantity, 1)
                  )
                """;
        int updated = jdbcTemplate.update(sql, itemId);
        return updated > 0;
    }
}
