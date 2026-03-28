package com.example.cep_backend.message.repository;

import com.example.cep_backend.message.dto.MessageConversationDto;
import com.example.cep_backend.message.dto.MessageItemDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

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
                        SELECT TOP 1 p.photo_url
                        FROM item_photos p
                        WHERE p.item_id = c.item_id
                        ORDER BY p.sort_order ASC, p.id ASC
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

    public boolean existsConversationForUser(Long conversationId, Long userId) {
        String sql = """
                SELECT COUNT(1)
                FROM message_conversations
                WHERE id = ? AND (buyer_user_id = ? OR seller_user_id = ?)
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, conversationId, userId, userId);
        return count != null && count > 0;
    }

    public List<MessageItemDto> findConversationMessages(Long conversationId, Long currentUserId) {
        String sql = """
                SELECT
                    m.id,
                    m.sender_user_id,
                    m.message_type,
                    m.text_content,
                    m.image_url,
                    m.created_at
                FROM message_records m
                INNER JOIN message_conversations c ON c.id = m.conversation_id
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

            if ((text == null || text.trim().isEmpty()) && "IMAGE".equalsIgnoreCase(messageType)) {
                text = "";
            }

            return new MessageItemDto(
                    rs.getLong("id"),
                    currentUserId.equals(senderUserId) ? "self" : "other",
                    text == null ? "" : text,
                    imageUrl == null ? "" : imageUrl,
                    createdAt.format(DATE_TIME_FORMATTER));
        }, conversationId, currentUserId, currentUserId);
    }
}
