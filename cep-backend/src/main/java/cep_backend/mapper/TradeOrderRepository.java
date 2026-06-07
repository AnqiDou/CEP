package cep_backend.mapper;

import cep_backend.entity.po.TradeOrderItemSnapshot;
import cep_backend.entity.po.TradeOrderRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class TradeOrderRepository {
    private final JdbcTemplate jdbcTemplate;
    private final boolean hasBuyerConfirmedColumn;
    private final boolean hasSellerConfirmedColumn;
    private final boolean hasRefundStatusColumn;
    private final boolean hasRefundTypeColumn;

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
            rs.getTimestamp("paid_at") == null ? null : rs.getTimestamp("paid_at").toLocalDateTime(),
            rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toLocalDateTime());

    public TradeOrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.hasBuyerConfirmedColumn = hasTradeOrdersColumn("buyer_confirmed");
        this.hasSellerConfirmedColumn = hasTradeOrdersColumn("seller_confirmed");
        this.hasRefundStatusColumn = hasTradeOrdersColumn("refund_status");
        this.hasRefundTypeColumn = hasTradeOrdersColumn("refund_type");
    }

    private boolean hasTradeOrdersColumn(String columnName) {
        String sql = """
                SELECT COUNT(1)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'trade_orders'
                  AND column_name = ?
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, columnName);
        return count != null && count > 0;
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

    public int cancelTimedOutPendingOrders(int timeoutMinutes) {
        String sql = """
                UPDATE trade_orders
                SET status = 'CANCELLED',
                    completed_at = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE status = 'PENDING_PAYMENT'
                  AND created_at < DATE_SUB(CURRENT_TIMESTAMP, INTERVAL ? MINUTE)
                """;
        return jdbcTemplate.update(sql, timeoutMinutes);
    }

    public TradeOrderRecord findLatestPendingOrderByBuyerAndItem(Long buyerUserId, Long itemId) {
        String buyerConfirmedSelect = hasBuyerConfirmedColumn
                ? "buyer_confirmed"
                : "FALSE AS buyer_confirmed";
        String sellerConfirmedSelect = hasSellerConfirmedColumn
                ? "seller_confirmed"
                : "FALSE AS seller_confirmed";
        String refundStatusSelect = hasRefundStatusColumn
                ? "refund_status"
                : "'NONE' AS refund_status";
        String refundTypeSelect = hasRefundTypeColumn
                ? "refund_type"
                : "NULL AS refund_type";
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
                    %s,
                    %s,
                    %s,
                    %s,
                    created_at,
                    paid_at,
                    completed_at
                FROM trade_orders
                WHERE buyer_user_id = ?
                  AND item_id = ?
                  AND status = 'PENDING_PAYMENT'
                ORDER BY created_at DESC, id DESC
                LIMIT 1
                """.formatted(
                buyerConfirmedSelect,
                sellerConfirmedSelect,
                refundStatusSelect,
                refundTypeSelect);
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
        String buyerConfirmedSelect = hasBuyerConfirmedColumn
                ? "buyer_confirmed"
                : "FALSE AS buyer_confirmed";
        String sellerConfirmedSelect = hasSellerConfirmedColumn
                ? "seller_confirmed"
                : "FALSE AS seller_confirmed";
        String refundStatusSelect = hasRefundStatusColumn
                ? "refund_status"
                : "'NONE' AS refund_status";
        String refundTypeSelect = hasRefundTypeColumn
                ? "refund_type"
                : "NULL AS refund_type";
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
                    %s,
                    %s,
                    %s,
                    %s,
                    created_at,
                    paid_at,
                    completed_at
                FROM trade_orders
                WHERE id = ?
                """.formatted(
                buyerConfirmedSelect,
                sellerConfirmedSelect,
                refundStatusSelect,
                refundTypeSelect);

        List<TradeOrderRecord> records = jdbcTemplate.query(sql, tradeOrderRowMapper, orderId);
        return records.isEmpty() ? null : records.getFirst();
    }

    public int markOrderPaid(Long orderId, LocalDateTime paidAt) {
        StringBuilder setClause = new StringBuilder();
        setClause.append("status = 'PENDING_CONFIRMATION', paid_at = ?");
        if (hasBuyerConfirmedColumn) {
            setClause.append(", buyer_confirmed = FALSE");
        }
        if (hasSellerConfirmedColumn) {
            setClause.append(", seller_confirmed = FALSE");
        }
        if (hasRefundStatusColumn) {
            setClause.append(", refund_status = 'NONE'");
        }
        if (hasRefundTypeColumn) {
            setClause.append(", refund_type = NULL");
        }
        setClause.append(", completed_at = NULL, updated_at = CURRENT_TIMESTAMP");

        String sql = """
                UPDATE trade_orders
                SET %s
                WHERE id = ? AND status = 'PENDING_PAYMENT'
                """.formatted(setClause);
        return jdbcTemplate.update(sql, Timestamp.valueOf(paidAt), orderId);
    }

    public int markSellerConfirmedDelivery(Long orderId) {
        if (!hasSellerConfirmedColumn) {
            String sql = """
                    UPDATE trade_orders
                    SET status = 'COMPLETED',
                        completed_at = COALESCE(completed_at, CURRENT_TIMESTAMP),
                        updated_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                      AND status = 'PENDING_CONFIRMATION'
                    """;
            return jdbcTemplate.update(sql, orderId);
        }
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
        if (!hasBuyerConfirmedColumn) {
            String sql = """
                    UPDATE trade_orders
                    SET status = 'COMPLETED',
                        completed_at = COALESCE(completed_at, CURRENT_TIMESTAMP),
                        updated_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                      AND status = 'PENDING_CONFIRMATION'
                    """;
            return jdbcTemplate.update(sql, orderId);
        }
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
        if (!hasBuyerConfirmedColumn || !hasSellerConfirmedColumn) {
            String sql = """
                    UPDATE trade_orders
                    SET status = 'COMPLETED',
                        completed_at = CURRENT_TIMESTAMP,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                      AND status = 'PENDING_CONFIRMATION'
                    """;
            return jdbcTemplate.update(sql, orderId);
        }
        String sql = """
                UPDATE trade_orders
                SET status = 'COMPLETED',
                    completed_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND status = 'PENDING_CONFIRMATION'
                  AND buyer_confirmed = TRUE
                  AND seller_confirmed = TRUE
                """;
        return jdbcTemplate.update(sql, orderId);
    }

    public int completeOrderWhenBuyerConfirmed(Long orderId) {
        if (!hasBuyerConfirmedColumn) {
            String sql = """
                    UPDATE trade_orders
                    SET status = 'COMPLETED',
                        completed_at = CURRENT_TIMESTAMP,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                      AND status = 'PENDING_CONFIRMATION'
                    """;
            return jdbcTemplate.update(sql, orderId);
        }
        String sql = """
                UPDATE trade_orders
                SET status = 'COMPLETED',
                    completed_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND status = 'PENDING_CONFIRMATION'
                  AND buyer_confirmed = TRUE
                """;
        return jdbcTemplate.update(sql, orderId);
    }

    public int applyRefund(Long orderId, String refundType) {
        if (!hasRefundStatusColumn || !hasRefundTypeColumn) {
            return 0;
        }
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
        if (!hasRefundStatusColumn || !hasRefundTypeColumn) {
            return 0;
        }
        String sql = """
                UPDATE trade_orders
                SET status = 'COMPLETED',
                    refund_status = 'APPROVED',
                    completed_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND status = 'PENDING_CONFIRMATION'
                  AND refund_status = 'APPLIED'
                  AND refund_type = 'NO_RECEIPT'
                """;
        return jdbcTemplate.update(sql, orderId);
    }

    public int approveRefundAfterReturn(Long orderId) {
        if (!hasRefundStatusColumn || !hasRefundTypeColumn) {
            return 0;
        }
        String sql = """
                UPDATE trade_orders
                SET refund_status = 'APPROVED',
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND status = 'PENDING_CONFIRMATION'
                  AND refund_status = 'APPLIED'
                  AND refund_type = 'RETURN_AFTER_RECEIPT'
                """;
        return jdbcTemplate.update(sql, orderId);
    }

    public int approveRefundPlatformIntervention(Long orderId) {
        if (!hasRefundStatusColumn || !hasRefundTypeColumn) {
            return 0;
        }
        String sql = """
                UPDATE trade_orders
                SET status = 'COMPLETED',
                    refund_status = 'APPROVED',
                    completed_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND status = 'PENDING_CONFIRMATION'
                  AND refund_status = 'APPLIED'
                  AND refund_type = 'PLATFORM_INTERVENTION'
                """;
        return jdbcTemplate.update(sql, orderId);
    }

    public int rejectRefund(Long orderId) {
        if (!hasRefundStatusColumn || !hasRefundTypeColumn) {
            return 0;
        }
        String sql = """
                UPDATE trade_orders
                SET status = CASE
                        WHEN refund_type = 'RETURN_AFTER_RECEIPT' THEN 'COMPLETED'
                        ELSE status
                    END,
                    completed_at = CASE
                        WHEN refund_type = 'RETURN_AFTER_RECEIPT' THEN COALESCE(completed_at, CURRENT_TIMESTAMP)
                        ELSE completed_at
                    END,
                    refund_status = CASE
                        WHEN refund_type = 'RETURN_AFTER_RECEIPT' THEN 'REJECTED'
                        ELSE 'NONE'
                    END,
                    refund_type = CASE
                        WHEN refund_type = 'RETURN_AFTER_RECEIPT' THEN refund_type
                        ELSE NULL
                    END,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND status = 'PENDING_CONFIRMATION'
                  AND refund_status = 'APPLIED'
                """;
        return jdbcTemplate.update(sql, orderId);
    }

    public int withdrawRejectedRefund(Long orderId) {
        if (!hasRefundStatusColumn || !hasRefundTypeColumn) {
            return 0;
        }
        String sql = """
                UPDATE trade_orders
                SET refund_status = 'NONE',
                    refund_type = 'REJECTED_WITHDRAWN',
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND status = 'PENDING_CONFIRMATION'
                  AND refund_status = 'REJECTED'
                """;
        return jdbcTemplate.update(sql, orderId);
    }

    public int escalateRejectedRefundToPlatform(Long orderId) {
        if (!hasRefundStatusColumn || !hasRefundTypeColumn) {
            return 0;
        }
        String sql = """
                UPDATE trade_orders
                SET refund_status = 'APPLIED',
                    refund_type = 'PLATFORM_INTERVENTION',
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND status = 'PENDING_CONFIRMATION'
                  AND refund_status = 'REJECTED'
                """;
        return jdbcTemplate.update(sql, orderId);
    }

    public int cancelPendingOrder(Long orderId, Long actorUserId) {
        String sql = """
                UPDATE trade_orders
                SET status = 'CANCELLED',
                    completed_at = NULL,
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
                    completed_at = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE buyer_user_id = ?
                  AND item_id = ?
                  AND status = 'PENDING_PAYMENT'
                """;
        return jdbcTemplate.update(sql, buyerUserId, itemId);
    }

    public String findLatestOrderNoByDatePrefix(String orderNoPrefix) {
        String sql = """
                SELECT order_no
                FROM trade_orders
                WHERE order_no LIKE ?
                  AND CHAR_LENGTH(order_no) = ?
                  AND SUBSTRING(order_no, ?) REGEXP '^[0-9]{6}$'
                ORDER BY order_no DESC
                LIMIT 1
                """;
        int expectedLength = orderNoPrefix.length() + 6;
        int sequenceStartPosition = orderNoPrefix.length() + 1;
        List<String> list = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getString("order_no"),
                orderNoPrefix + "%",
                expectedLength,
                sequenceStartPosition);
        return list.isEmpty() ? null : list.getFirst();
    }

    public TradeOrderDetailRecord findOrderDetailByIdForUser(Long orderId, Long actorUserId) {
        String refundStatusSelect = hasRefundStatusColumn
                ? "o.refund_status"
                : "'NONE' AS refund_status";
        String sql = """
                SELECT
                    o.id AS order_id,
                    o.order_no,
                    o.status,
                    %s,
                    o.created_at,
                    o.paid_at,
                    o.completed_at,
                    o.item_id,
                    COALESCE(NULLIF(o.item_title, ''), '未命名物品') AS item_title,
                    COALESCE(NULLIF(i.description, ''), '') AS item_description,
                    COALESCE(
                        NULLIF(o.cover_photo_url, ''),
                        (
                            SELECT p.photo_url
                            FROM item_photos p
                            WHERE p.item_id = o.item_id
                            ORDER BY p.sort_order ASC, p.id ASC
                            LIMIT 1
                        ),
                        ''
                    ) AS item_image,
                    o.amount,
                    o.buyer_user_id,
                    COALESCE(NULLIF(bu.username, ''), '校园用户') AS buyer_name,
                    COALESCE(NULLIF(bup.avatar_url, ''), '') AS buyer_avatar,
                    COALESCE(NULLIF(o.receiver_phone, ''), NULLIF(bup.phone, ''), '') AS buyer_phone,
                    o.seller_user_id,
                    COALESCE(NULLIF(su.username, ''), '校园用户') AS seller_name,
                    COALESCE(NULLIF(sup.avatar_url, ''), '') AS seller_avatar,
                    COALESCE(NULLIF(sup.phone, ''), '') AS seller_phone
                FROM trade_orders o
                LEFT JOIN items i ON i.id = o.item_id
                LEFT JOIN users bu ON bu.id = o.buyer_user_id
                LEFT JOIN user_profiles bup ON bup.user_id = o.buyer_user_id
                LEFT JOIN users su ON su.id = o.seller_user_id
                LEFT JOIN user_profiles sup ON sup.user_id = o.seller_user_id
                WHERE o.id = ?
                  AND (o.buyer_user_id = ? OR o.seller_user_id = ?)
                LIMIT 1
                """.formatted(refundStatusSelect);
        List<TradeOrderDetailRecord> list = jdbcTemplate.query(sql, (rs, rowNum) -> new TradeOrderDetailRecord(
                rs.getLong("order_id"),
                rs.getString("order_no"),
                rs.getString("status"),
                rs.getString("refund_status"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("paid_at") == null ? null : rs.getTimestamp("paid_at").toLocalDateTime(),
                rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toLocalDateTime(),
                rs.getLong("item_id"),
                rs.getString("item_title"),
                rs.getString("item_description"),
                rs.getString("item_image"),
                rs.getBigDecimal("amount"),
                rs.getObject("buyer_user_id", Long.class),
                rs.getString("buyer_name"),
                rs.getString("buyer_avatar"),
                rs.getString("buyer_phone"),
                rs.getObject("seller_user_id", Long.class),
                rs.getString("seller_name"),
                rs.getString("seller_avatar"),
                rs.getString("seller_phone")),
                orderId,
                actorUserId,
                actorUserId);
        return list.isEmpty() ? null : list.getFirst();
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
                        WHEN (COALESCE(sold_quantity, 0) + 1) >= COALESCE(total_quantity, 1) THEN 'SOLD_OUT'
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

    public record TradeOrderDetailRecord(
            Long orderId,
            String orderNo,
            String status,
            String refundStatus,
            LocalDateTime createdAt,
            LocalDateTime paidAt,
            LocalDateTime completedAt,
            Long itemId,
            String itemTitle,
            String itemDescription,
            String itemImage,
            BigDecimal amount,
            Long buyerUserId,
            String buyerName,
            String buyerAvatar,
            String buyerPhone,
            Long sellerUserId,
            String sellerName,
            String sellerAvatar,
            String sellerPhone) {
    }
}
