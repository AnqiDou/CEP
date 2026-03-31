package com.example.cep_backend.admin.repository;

import com.example.cep_backend.admin.dto.AdminItemDto;
import com.example.cep_backend.admin.dto.AdminNoticeDto;
import com.example.cep_backend.admin.dto.AdminOrderDto;
import com.example.cep_backend.admin.dto.AdminSupportConversationDto;
import com.example.cep_backend.admin.dto.AdminSupportMessageDto;
import com.example.cep_backend.admin.dto.AdminUserDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AdminRepository {
    private final JdbcTemplate jdbcTemplate;

    public AdminRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Integer countTodayNewUsers() {
        String sql = """
                SELECT COUNT(1)
                FROM users
                WHERE CAST(created_at AS DATE) = CAST(CURRENT_TIMESTAMP AS DATE)
                  AND status <> 'DELETED'
                """;
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    public Integer countTotalUsers() {
        String sql = "SELECT COUNT(1) FROM users WHERE status <> 'DELETED'";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    public Integer countTodayNewItems() {
        String sql = """
                SELECT COUNT(1)
                FROM items
                WHERE CAST(created_at AS DATE) = CAST(CURRENT_TIMESTAMP AS DATE)
                  AND status <> 'DELETED'
                """;
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    public Integer countTotalItems() {
        String sql = "SELECT COUNT(1) FROM items WHERE status <> 'DELETED'";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    public Integer countTodayOrders() {
        String sql = """
                SELECT COUNT(1)
                FROM trade_orders
                WHERE CAST(created_at AS DATE) = CAST(CURRENT_TIMESTAMP AS DATE)
                """;
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    public java.math.BigDecimal sumTodaySales() {
        String sql = """
                SELECT IFNULL(SUM(amount), 0)
                FROM trade_orders
                WHERE CAST(created_at AS DATE) = CAST(CURRENT_TIMESTAMP AS DATE)
                  AND status = 'PAID'
                """;
        return jdbcTemplate.queryForObject(sql, java.math.BigDecimal.class);
    }

    public Integer countPendingItems() {
        String sql = "SELECT COUNT(1) FROM items WHERE status = 'PENDING_REVIEW'";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    public Integer countAbnormalOrders() {
        String sql = "SELECT COUNT(1) FROM trade_orders WHERE status <> 'PAID'";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    public Integer countPendingConversations() {
        String sql = """
                SELECT COUNT(1)
                FROM admin_support_conversations
                WHERE status = 'OPEN'
                """;
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    public Map<String, Integer> countOrderStates() {
        String sql = """
                SELECT status, COUNT(1) AS count_value
                FROM trade_orders
                GROUP BY status
                """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String key = String.valueOf(row.get("status"));
            Number value = (Number) row.get("count_value");
            result.put(key, value == null ? 0 : value.intValue());
        }
        return result;
    }

    public List<AdminUserDto> listUsers(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim();
        String sql = """
                SELECT
                    u.id,
                    COALESCE(NULLIF(u.username, ''), u.email) AS display_name,
                    p.note AS phone,
                    u.email,
                    u.created_at,
                    CASE WHEN u.status = 'DISABLED' THEN 1 ELSE 0 END AS disabled,
                    (
                        SELECT COUNT(1) FROM items i
                        WHERE COALESCE(i.publisher_user_id, (
                            SELECT d.publisher_user_id FROM item_details d WHERE d.item_id = i.id ORDER BY d.id DESC LIMIT 1
                        )) = u.id
                          AND i.status <> 'DELETED'
                    ) AS item_count,
                    (
                        SELECT COUNT(1) FROM trade_orders t
                        WHERE t.buyer_user_id = u.id OR t.seller_user_id = u.id
                    ) AS order_count
                FROM users u
                LEFT JOIN user_profiles p ON p.user_id = u.id
                WHERE u.status <> 'DELETED'
                  AND (? = '' OR u.email LIKE ? OR u.username LIKE ?)
                ORDER BY u.created_at DESC, u.id DESC
                """;
        String like = "%" + normalized + "%";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AdminUserDto(
                rs.getLong("id"),
                rs.getString("display_name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getBoolean("disabled"),
                rs.getInt("item_count"),
                rs.getInt("order_count")), normalized, like, like);
    }

    public int updateUserStatus(Long userId, String status, LocalDateTime now) {
        String sql = """
                UPDATE users
                SET status = ?,
                    updated_at = ?
                WHERE id = ?
                  AND status <> 'DELETED'
                """;
        return jdbcTemplate.update(sql, status, Timestamp.valueOf(now), userId);
    }

    public int deleteUser(Long userId, LocalDateTime now) {
        String sql = """
                UPDATE users
                SET status = 'DELETED',
                    updated_at = ?
                WHERE id = ?
                  AND status <> 'DELETED'
                """;
        return jdbcTemplate.update(sql, Timestamp.valueOf(now), userId);
    }

    public List<AdminItemDto> listItems(String keyword, String status) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String normalizedStatus = status == null ? "all" : status.trim().toLowerCase();
        String itemStatus = mapItemStatus(normalizedStatus);
        String sql = """
                SELECT
                    i.id,
                    i.title,
                    c.name AS category_name,
                    i.price,
                    COALESCE(NULLIF(u.username, ''), u.email, '未知用户') AS owner_name,
                    i.status
                FROM items i
                INNER JOIN item_categories c ON c.id = i.category_id
                LEFT JOIN users u ON u.id = COALESCE(i.publisher_user_id, (
                    SELECT d.publisher_user_id
                    FROM item_details d
                    WHERE d.item_id = i.id
                    ORDER BY d.id DESC
                    LIMIT 1
                ))
                WHERE i.status <> 'DELETED'
                  AND (? = '' OR i.title LIKE ? OR c.name LIKE ?)
                  AND (? = 'ALL' OR i.status = ?)
                ORDER BY i.created_at DESC, i.id DESC
                """;
        String like = "%" + normalizedKeyword + "%";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AdminItemDto(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("category_name"),
                rs.getBigDecimal("price"),
                rs.getString("owner_name"),
                rs.getString("status")), normalizedKeyword, like, like, itemStatus, itemStatus);
    }

    public int updateItemStatus(Long itemId, String status, LocalDateTime now) {
        String sql = """
                UPDATE items
                SET status = ?,
                    updated_at = ?
                WHERE id = ?
                  AND status <> 'DELETED'
                """;
        return jdbcTemplate.update(sql, status, Timestamp.valueOf(now), itemId);
    }

    public int deleteItem(Long itemId, LocalDateTime now) {
        String sql = """
                UPDATE items
                SET status = 'DELETED',
                    updated_at = ?
                WHERE id = ?
                  AND status <> 'DELETED'
                """;
        return jdbcTemplate.update(sql, Timestamp.valueOf(now), itemId);
    }

    public List<AdminOrderDto> listOrders(String keyword, String status) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String normalizedStatus = status == null ? "all" : status.trim().toLowerCase();
        String orderStatus = mapOrderStatus(normalizedStatus);
        String sql = """
                SELECT
                    o.order_no,
                    o.item_title,
                    COALESCE(NULLIF(b.username, ''), b.email, '未知买家') AS buyer_name,
                    COALESCE(NULLIF(s.username, ''), s.email, '未知卖家') AS seller_name,
                    o.amount,
                    o.status
                FROM trade_orders o
                LEFT JOIN users b ON b.id = o.buyer_user_id
                LEFT JOIN users s ON s.id = o.seller_user_id
                WHERE (? = '' OR o.order_no LIKE ? OR o.item_title LIKE ?
                    OR b.email LIKE ? OR b.username LIKE ?
                    OR s.email LIKE ? OR s.username LIKE ?)
                  AND (? = 'ALL' OR o.status = ?)
                ORDER BY o.created_at DESC, o.id DESC
                """;
        String like = "%" + normalizedKeyword + "%";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AdminOrderDto(
                rs.getString("order_no"),
                rs.getString("item_title"),
                rs.getString("buyer_name"),
                rs.getString("seller_name"),
                rs.getBigDecimal("amount"),
                rs.getString("status")), normalizedKeyword, like, like, like, like, like, like, orderStatus,
                orderStatus);
    }

    public int markOrderHandled(String orderNo, LocalDateTime now) {
        String sql = """
                UPDATE trade_orders
                SET status = 'PAID',
                    paid_at = COALESCE(paid_at, ?),
                    updated_at = ?
                WHERE order_no = ?
                """;
        Timestamp timestamp = Timestamp.valueOf(now);
        return jdbcTemplate.update(sql, timestamp, timestamp, orderNo);
    }

    public List<AdminSupportConversationDto> listSupportConversations() {
        String sql = """
                SELECT
                    c.id AS conversation_id,
                    c.title,
                    c.preview,
                    m.id AS message_id,
                    m.sender_type,
                    m.content,
                    m.created_at
                FROM admin_support_conversations c
                LEFT JOIN admin_support_messages m ON m.conversation_id = c.id
                ORDER BY c.updated_at DESC, c.id DESC, m.created_at ASC, m.id ASC
                """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        Map<Long, ConversationAccumulator> accumulators = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            Long conversationId = ((Number) row.get("conversation_id")).longValue();
            ConversationAccumulator accumulator = accumulators.computeIfAbsent(
                    conversationId,
                    id -> new ConversationAccumulator(
                            id,
                            String.valueOf(row.get("title")),
                            String.valueOf(row.get("preview")),
                            new ArrayList<>()));

            Number messageId = (Number) row.get("message_id");
            if (messageId != null) {
                String senderType = String.valueOf(row.get("sender_type"));
                String from = "ADMIN".equalsIgnoreCase(senderType) ? "管理员" : "用户";
                Timestamp createdAt = (Timestamp) row.get("created_at");
                accumulator.messages().add(new AdminSupportMessageDto(
                        messageId.longValue(),
                        from,
                        String.valueOf(row.get("content")),
                        createdAt == null ? null : createdAt.toLocalDateTime()));
            }
        }

        return accumulators.values().stream()
                .map(item -> new AdminSupportConversationDto(
                        item.id(),
                        item.title(),
                        item.preview(),
                        item.messages()))
                .toList();
    }

    public int insertSupportMessage(Long conversationId, String senderType, String content, LocalDateTime now) {
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

    public int touchConversation(Long conversationId, String preview, LocalDateTime now) {
        String sql = """
                UPDATE admin_support_conversations
                SET preview = ?,
                    updated_at = ?
                WHERE id = ?
                """;
        return jdbcTemplate.update(sql, preview, Timestamp.valueOf(now), conversationId);
    }

    public List<AdminNoticeDto> listNotices() {
        String sql = """
                SELECT id, content, created_at
                FROM admin_notices
                ORDER BY created_at DESC, id DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AdminNoticeDto(
                rs.getLong("id"),
                rs.getString("content"),
                rs.getTimestamp("created_at").toLocalDateTime()));
    }

    public int createNotice(String content, LocalDateTime now) {
        String sql = """
                INSERT INTO admin_notices (content, created_at, updated_at)
                VALUES (?, ?, ?)
                """;
        Timestamp timestamp = Timestamp.valueOf(now);
        return jdbcTemplate.update(sql, content, timestamp, timestamp);
    }

    public int deleteNotice(Long noticeId) {
        String sql = "DELETE FROM admin_notices WHERE id = ?";
        return jdbcTemplate.update(sql, noticeId);
    }

    private String mapItemStatus(String status) {
        return switch (status) {
            case "pending" -> "PENDING_REVIEW";
            case "online" -> "PUBLISHED";
            case "offline" -> "OFF_SHELF";
            default -> "ALL";
        };
    }

    private String mapOrderStatus(String status) {
        return switch (status) {
            case "pending-pay" -> "PENDING_PAYMENT";
            case "completed" -> "PAID";
            case "cancelled" -> "CANCELLED";
            default -> "ALL";
        };
    }

    private record ConversationAccumulator(
            Long id,
            String title,
            String preview,
            List<AdminSupportMessageDto> messages) {
    }
}
