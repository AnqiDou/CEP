package com.example.cep_backend.message.repository;

import com.example.cep_backend.message.dto.MessageConversationDto;
import com.example.cep_backend.message.dto.MessageItemDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Repository
public class MessageRepository {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<MessageConversationDto> conversationRowMapper = (rs, rowNum) -> {
        LocalDateTime lastMessageAt = rs.getTimestamp("last_message_at").toLocalDateTime();
        String lastMessage = rs.getString("last_message");
        String messageType = rs.getString("last_message_type");
        if ((lastMessage == null || lastMessage.trim().isEmpty()) && "IMAGE".equalsIgnoreCase(messageType)) {
            lastMessage = "[图片]";
        }
        return new MessageConversationDto(
                rs.getLong("id"),
                rs.getLong("peer_user_id"),
                rs.getString("peer_name"),
                rs.getString("peer_avatar"),
                rs.getLong("item_id"),
                rs.getString("item_title"),
                rs.getString("item_image"),
                rs.getInt("unread_count"),
                lastMessage == null ? "" : lastMessage,
                lastMessageAt.format(DATE_TIME_FORMATTER));
    };

    public MessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MessageConversationDto> findConversations(Long userId, String filter) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    c.id,
                    CASE WHEN c.buyer_user_id = ? THEN c.seller_user_id ELSE c.buyer_user_id END AS peer_user_id,
                    COALESCE(NULLIF(u.username, ''), '校园用户') AS peer_name,
                    up.avatar_url AS peer_avatar,
                    c.item_id,
                    i.title AS item_title,
                    (
                        SELECT p.photo_url
                        FROM item_photos p
                        WHERE p.item_id = c.item_id
                        ORDER BY p.sort_order ASC, p.id ASC
                        LIMIT 1
                    ) AS item_image,
                    CASE WHEN c.buyer_user_id = ? THEN c.unread_buyer ELSE c.unread_seller END AS unread_count,
                    c.last_message,
                    c.last_message_type,
                    c.last_message_at
                FROM message_conversations c
                LEFT JOIN users u ON u.id = CASE WHEN c.buyer_user_id = ? THEN c.seller_user_id ELSE c.buyer_user_id END
                LEFT JOIN user_profiles up ON up.user_id = u.id
                LEFT JOIN items i ON i.id = c.item_id
                WHERE (c.buyer_user_id = ? OR c.seller_user_id = ?)
                """);
        if ("unread".equals(filter)) {
            sql.append(" AND (CASE WHEN c.buyer_user_id = ? THEN c.unread_buyer ELSE c.unread_seller END) > 0 ");
        }
        if ("read".equals(filter)) {
            sql.append(" AND (CASE WHEN c.buyer_user_id = ? THEN c.unread_buyer ELSE c.unread_seller END) = 0 ");
        }
        sql.append(" ORDER BY c.last_message_at DESC, c.id DESC ");

        if ("all".equals(filter)) {
            return jdbcTemplate.query(sql.toString(), conversationRowMapper, userId, userId, userId, userId, userId);
        }
        return jdbcTemplate.query(sql.toString(), conversationRowMapper, userId, userId, userId, userId, userId,
                userId);
    }

    public MessageConversationDto findConversationByIdForUser(Long userId, Long conversationId) {
        String sql = """
                SELECT
                    c.id,
                    CASE WHEN c.buyer_user_id = ? THEN c.seller_user_id ELSE c.buyer_user_id END AS peer_user_id,
                    COALESCE(NULLIF(u.username, ''), '校园用户') AS peer_name,
                    up.avatar_url AS peer_avatar,
                    c.item_id,
                    i.title AS item_title,
                    (
                        SELECT p.photo_url
                        FROM item_photos p
                        WHERE p.item_id = c.item_id
                        ORDER BY p.sort_order ASC, p.id ASC
                        LIMIT 1
                    ) AS item_image,
                    CASE WHEN c.buyer_user_id = ? THEN c.unread_buyer ELSE c.unread_seller END AS unread_count,
                    c.last_message,
                    c.last_message_type,
                    c.last_message_at
                FROM message_conversations c
                LEFT JOIN users u ON u.id = CASE WHEN c.buyer_user_id = ? THEN c.seller_user_id ELSE c.buyer_user_id END
                LEFT JOIN user_profiles up ON up.user_id = u.id
                LEFT JOIN items i ON i.id = c.item_id
                WHERE c.id = ?
                  AND (c.buyer_user_id = ? OR c.seller_user_id = ?)
                """;
        List<MessageConversationDto> list = jdbcTemplate.query(
                sql,
                conversationRowMapper,
                userId,
                userId,
                userId,
                conversationId,
                userId,
                userId);
        return list.isEmpty() ? null : list.getFirst();
    }

    public boolean existsConversationForUser(Long conversationId, Long userId) {
        String sql = """
                SELECT COUNT(1)
                FROM message_conversations
                WHERE id = ? AND (buyer_user_id = ? OR seller_user_id = ?)
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, conversationId, userId, userId);
        return count != null && count > 0;
    }

    public ConversationParticipants findConversationParticipantsForUser(Long conversationId, Long userId) {
        String sql = """
                SELECT
                    id,
                    item_id,
                    buyer_user_id,
                    seller_user_id
                FROM message_conversations
                WHERE id = ?
                  AND (buyer_user_id = ? OR seller_user_id = ?)
                LIMIT 1
                """;
        List<ConversationParticipants> list = jdbcTemplate.query(sql,
                (rs, rowNum) -> new ConversationParticipants(
                        rs.getLong("id"),
                        rs.getLong("item_id"),
                        rs.getLong("buyer_user_id"),
                        rs.getLong("seller_user_id")),
                conversationId,
                userId,
                userId);
        return list.isEmpty() ? null : list.getFirst();
    }

    public Long findItemSellerUserId(Long itemId) {
        String sql = """
                SELECT COALESCE(i.publisher_user_id, d.publisher_user_id) AS seller_user_id
                FROM items i
                LEFT JOIN item_details d ON d.item_id = i.id
                WHERE i.id = ?
                ORDER BY d.id DESC
                LIMIT 1
                """;
        List<Long> list = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getObject("seller_user_id", Long.class), itemId);
        if (list.isEmpty()) {
            return null;
        }
        return list.getFirst();
    }

    public Long findConversationIdByItemAndPair(Long itemId, Long buyerUserId, Long sellerUserId) {
        String sql = """
                SELECT id
                FROM message_conversations
                WHERE item_id = ?
                  AND buyer_user_id = ?
                  AND seller_user_id = ?
                LIMIT 1
                """;
        List<Long> list = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("id"), itemId, buyerUserId, sellerUserId);
        return list.isEmpty() ? null : list.getFirst();
    }

    public Long createConversation(Long itemId, Long buyerUserId, Long sellerUserId, LocalDateTime now) {
        String sql = """
                INSERT INTO message_conversations (
                    item_id,
                    buyer_user_id,
                    seller_user_id,
                    last_message,
                    last_message_type,
                    unread_buyer,
                    unread_seller,
                    last_message_at,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, '', 'TEXT', 0, 0, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, itemId);
            statement.setLong(2, buyerUserId);
            statement.setLong(3, sellerUserId);
            statement.setTimestamp(4, Timestamp.valueOf(now));
            statement.setTimestamp(5, Timestamp.valueOf(now));
            statement.setTimestamp(6, Timestamp.valueOf(now));
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public List<MessageItemDto> findConversationMessages(Long conversationId, Long currentUserId) {
        String sql = """
                SELECT
                    m.id,
                    m.sender_user_id,
                    m.message_type,
                    m.text_content,
                    m.image_url,
                    m.created_at,
                    CASE
                        WHEN m.message_type = 'REVIEW_INVITE' AND m.biz_type = 'TRADE_ORDER_REVIEW' THEN m.biz_id
                        ELSE NULL
                    END AS review_order_id,
                    CASE
                        WHEN m.message_type = 'REVIEW_INVITE' THEN t.status
                        ELSE NULL
                    END AS review_status
                FROM message_records m
                INNER JOIN message_conversations c ON c.id = m.conversation_id
                LEFT JOIN trade_review_tasks t
                    ON m.message_type = 'REVIEW_INVITE'
                    AND m.biz_type = 'TRADE_ORDER_REVIEW'
                    AND t.order_id = m.biz_id
                    AND t.reviewer_user_id = ?
                WHERE m.conversation_id = ?
                  AND (c.buyer_user_id = ? OR c.seller_user_id = ?)
                ORDER BY m.created_at ASC, m.id ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp timestamp = rs.getTimestamp("created_at");
            LocalDateTime createdAt = timestamp == null ? LocalDateTime.now() : timestamp.toLocalDateTime();
            Long senderUserId = rs.getLong("sender_user_id");
            String messageType = rs.getString("message_type");
            String text = rs.getString("text_content");
            String imageUrl = rs.getString("image_url");
            Long reviewOrderId = rs.getObject("review_order_id", Long.class);
            String reviewStatus = rs.getString("review_status");

            if ((text == null || text.trim().isEmpty()) && "IMAGE".equalsIgnoreCase(messageType)) {
                text = "";
            }

            return new MessageItemDto(
                    rs.getLong("id"),
                    currentUserId.equals(senderUserId) ? "self" : "other",
                    text == null ? "" : text,
                    imageUrl == null ? "" : imageUrl,
                    createdAt.format(DATE_TIME_FORMATTER),
                    messageType == null ? "TEXT" : messageType,
                    reviewOrderId,
                    reviewStatus == null ? "" : reviewStatus);
        }, currentUserId, conversationId, currentUserId, currentUserId);
    }

    public void markConversationRead(Long conversationId, Long currentUserId) {
        String sql = """
                UPDATE message_conversations
                SET unread_buyer = CASE WHEN buyer_user_id = ? THEN 0 ELSE unread_buyer END,
                    unread_seller = CASE WHEN seller_user_id = ? THEN 0 ELSE unread_seller END,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND (buyer_user_id = ? OR seller_user_id = ?)
                """;
        jdbcTemplate.update(sql, currentUserId, currentUserId, conversationId, currentUserId, currentUserId);
    }

    public DeliveryRecord saveMessage(Long conversationId, Long senderUserId, String text, String imageUrl,
            LocalDateTime now) {
        ConversationParticipants participants = findConversationParticipantsForUser(conversationId, senderUserId);
        if (participants == null) {
            return null;
        }

        Long receiverUserId = participants.buyerUserId().equals(senderUserId)
                ? participants.sellerUserId()
                : participants.buyerUserId();
        String normalizedText = text == null ? "" : text.trim();
        String normalizedImageUrl = imageUrl == null ? "" : imageUrl.trim();
        String messageType = normalizedImageUrl.isEmpty() ? "TEXT" : "IMAGE";
        String preview = normalizedText.isEmpty() ? "[图片]" : normalizedText;

        String insertSql = """
                INSERT INTO message_records (
                    conversation_id,
                    sender_user_id,
                    message_type,
                    text_content,
                    image_url,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, conversationId);
            statement.setLong(2, senderUserId);
            statement.setString(3, messageType);
            statement.setString(4, normalizedText);
            statement.setString(5, normalizedImageUrl);
            statement.setTimestamp(6, Timestamp.valueOf(now));
            return statement;
        }, keyHolder);

        String updateSql = """
                UPDATE message_conversations
                SET last_message = ?,
                    last_message_type = ?,
                    unread_buyer = CASE WHEN buyer_user_id = ? THEN unread_buyer ELSE unread_buyer + 1 END,
                    unread_seller = CASE WHEN seller_user_id = ? THEN unread_seller ELSE unread_seller + 1 END,
                    last_message_at = ?,
                    updated_at = ?
                WHERE id = ?
                """;
        jdbcTemplate.update(updateSql,
                preview,
                messageType,
                senderUserId,
                senderUserId,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now),
                conversationId);

        Number key = keyHolder.getKey();
        if (key == null) {
            return null;
        }
        return new DeliveryRecord(key.longValue(), senderUserId, receiverUserId, conversationId);
    }

    public MessageItemDto findMessageItemForUser(Long messageId, Long currentUserId) {
        String sql = """
                SELECT
                    m.id,
                    m.sender_user_id,
                    m.message_type,
                    m.text_content,
                    m.image_url,
                    m.created_at,
                    CASE
                        WHEN m.message_type = 'REVIEW_INVITE' AND m.biz_type = 'TRADE_ORDER_REVIEW' THEN m.biz_id
                        ELSE NULL
                    END AS review_order_id,
                    CASE
                        WHEN m.message_type = 'REVIEW_INVITE' THEN t.status
                        ELSE NULL
                    END AS review_status
                FROM message_records m
                INNER JOIN message_conversations c ON c.id = m.conversation_id
                LEFT JOIN trade_review_tasks t
                    ON m.message_type = 'REVIEW_INVITE'
                    AND m.biz_type = 'TRADE_ORDER_REVIEW'
                    AND t.order_id = m.biz_id
                    AND t.reviewer_user_id = ?
                WHERE m.id = ?
                  AND (c.buyer_user_id = ? OR c.seller_user_id = ?)
                LIMIT 1
                """;
        List<MessageItemDto> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp timestamp = rs.getTimestamp("created_at");
            LocalDateTime createdAt = timestamp == null ? LocalDateTime.now() : timestamp.toLocalDateTime();
            Long senderUserId = rs.getLong("sender_user_id");
            String messageType = rs.getString("message_type");
            String text = rs.getString("text_content");
            String imageUrl = rs.getString("image_url");
            Long reviewOrderId = rs.getObject("review_order_id", Long.class);
            String reviewStatus = rs.getString("review_status");
            return new MessageItemDto(
                    rs.getLong("id"),
                    currentUserId.equals(senderUserId) ? "self" : "other",
                    text == null ? "" : text,
                    imageUrl == null ? "" : imageUrl,
                    createdAt.format(DATE_TIME_FORMATTER),
                    messageType == null ? "TEXT" : messageType,
                    reviewOrderId,
                    reviewStatus == null ? "" : reviewStatus);
        }, currentUserId, messageId, currentUserId, currentUserId);
        return list.isEmpty() ? null : list.getFirst();
    }

    public record ConversationParticipants(
            Long conversationId,
            Long itemId,
            Long buyerUserId,
            Long sellerUserId) {
    }

    public record DeliveryRecord(
            Long messageId,
            Long senderUserId,
            Long receiverUserId,
            Long conversationId) {
    }
}
