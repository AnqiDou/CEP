package com.example.cep_backend.admin.service;

import com.example.cep_backend.admin.dto.AdminDashboardDto;
import com.example.cep_backend.admin.dto.AdminItemDto;
import com.example.cep_backend.admin.dto.AdminNoticeDto;
import com.example.cep_backend.admin.dto.AdminOrderDto;
import com.example.cep_backend.admin.dto.AdminOrderStateStatDto;
import com.example.cep_backend.admin.dto.AdminSupportConversationDto;
import com.example.cep_backend.admin.dto.AdminSupportMessageDto;
import com.example.cep_backend.admin.dto.AdminUserDto;
import com.example.cep_backend.admin.repository.AdminRepository;
import com.example.cep_backend.auth.BusinessException;
import com.example.cep_backend.auth.dto.AuthUserDto;
import com.example.cep_backend.message.dto.MessageConversationDto;
import com.example.cep_backend.message.dto.MessageItemDto;
import com.example.cep_backend.message.ws.MessageWebSocketNotifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    private final AdminRepository adminRepository;
    private final String adminEmail;
    private final MessageWebSocketNotifier messageWebSocketNotifier;

    public AdminService(AdminRepository adminRepository,
            MessageWebSocketNotifier messageWebSocketNotifier,
            @Value("${app.admin.email:3299166215@qq.com}") String adminEmail) {
        this.adminRepository = adminRepository;
        this.messageWebSocketNotifier = messageWebSocketNotifier;
        this.adminEmail = adminEmail == null ? "" : adminEmail.trim().toLowerCase();
    }

    public void ensureAdmin(AuthUserDto currentUser) {
        if (currentUser == null || currentUser.email() == null) {
            throw new BusinessException("管理员身份无效，请重新登录");
        }
        String email = currentUser.email().trim().toLowerCase();
        if (!email.equals(adminEmail)) {
            throw new BusinessException("仅管理员可访问该接口");
        }
    }

    public AdminDashboardDto dashboard() {
        int todayNewUsers = safeInt(adminRepository.countTodayNewUsers());
        int totalUsers = safeInt(adminRepository.countTotalUsers());
        int todayNewItems = safeInt(adminRepository.countTodayNewItems());
        int totalItems = safeInt(adminRepository.countTotalItems());
        int todayOrders = safeInt(adminRepository.countTodayOrders());
        BigDecimal todaySales = adminRepository.sumTodaySales();
        int pendingItemCount = safeInt(adminRepository.countPendingItems());
        int abnormalOrderCount = safeInt(adminRepository.countAbnormalOrders());
        int pendingConversationCount = safeInt(adminRepository.countPendingConversations());

        Map<String, Integer> rawStats = adminRepository.countOrderStates();
        int pendingPayCount = rawStats.getOrDefault("PENDING_PAYMENT", 0);
        int completedCount = rawStats.getOrDefault("PAID", 0);
        int cancelledCount = rawStats.getOrDefault("CANCELLED", 0);
        int total = Math.max(pendingPayCount + completedCount + cancelledCount, 1);

        List<AdminOrderStateStatDto> stats = List.of(
                new AdminOrderStateStatDto("待付款", pendingPayCount, pendingPayCount * 100 / total),
                new AdminOrderStateStatDto("已完成", completedCount, completedCount * 100 / total),
                new AdminOrderStateStatDto("已取消", cancelledCount, cancelledCount * 100 / total));

        return new AdminDashboardDto(
                todayNewUsers,
                totalUsers,
                todayNewItems,
                totalItems,
                todayOrders,
                todaySales == null ? BigDecimal.ZERO : todaySales,
                pendingItemCount,
                abnormalOrderCount,
                pendingConversationCount,
                stats);
    }

    public List<AdminUserDto> listUsers(String keyword, String username, String phone, String email) {
        return adminRepository.listUsers(keyword, username, phone, email);
    }

    @Transactional
    public void updateUserStatus(Long userId, Boolean disabled) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户参数无效");
        }
        String targetStatus = Boolean.TRUE.equals(disabled) ? "DISABLED" : "ACTIVE";
        int updated = adminRepository.updateUserStatus(userId, targetStatus, LocalDateTime.now());
        if (updated <= 0) {
            throw new BusinessException("用户不存在或已删除");
        }
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户参数无效");
        }
        int updated = adminRepository.deleteUser(userId, LocalDateTime.now());
        if (updated <= 0) {
            throw new BusinessException("用户不存在或已删除");
        }
    }

    public List<AdminItemDto> listItems(
            String keyword,
            String title,
            String category,
            String price,
            String publisher,
            String status) {
        return adminRepository.listItems(keyword, title, category, price, publisher, status).stream()
                .map(item -> new AdminItemDto(
                        item.id(),
                        item.title(),
                        item.category(),
                        item.price(),
                        item.owner(),
                        toItemStatus(item.status())))
                .toList();
    }

    @Transactional
    public void approveItem(Long itemId) {
        mutateItemStatus(itemId, "PUBLISHED");
    }

    @Transactional
    public void forceOffline(Long itemId) {
        mutateItemStatus(itemId, "OFF_SHELF");
    }

    @Transactional
    public void deleteItem(Long itemId) {
        if (itemId == null || itemId <= 0) {
            throw new BusinessException("商品参数无效");
        }
        int updated = adminRepository.deleteItem(itemId, LocalDateTime.now());
        if (updated <= 0) {
            throw new BusinessException("商品不存在或已删除");
        }
    }

    public List<AdminOrderDto> listOrders(
            String keyword,
            String orderNo,
            String buyer,
            String seller,
            String itemTitle,
            String status) {
        return adminRepository.listOrders(keyword, orderNo, buyer, seller, itemTitle, status).stream()
                .map(order -> new AdminOrderDto(
                        order.orderNo(),
                        order.itemTitle(),
                        order.buyer(),
                        order.seller(),
                        order.amount(),
                        toOrderStatus(order.status())))
                .toList();
    }

    @Transactional
    public void handleOrderAbnormal(String orderNo) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            throw new BusinessException("订单号不能为空");
        }
        int updated = adminRepository.markOrderHandled(orderNo.trim(), LocalDateTime.now());
        if (updated <= 0) {
            throw new BusinessException("订单不存在");
        }
    }

    public List<AdminSupportConversationDto> listConversations() {
        return adminRepository.listSupportConversations();
    }

    @Transactional
    public void replyConversation(Long conversationId, String content, String imageUrl) {
        if (conversationId == null || conversationId <= 0) {
            throw new BusinessException("会话参数无效");
        }
        if (!adminRepository.existsSupportConversation(conversationId)) {
            throw new BusinessException("会话不存在");
        }
        String trimmed = content == null ? "" : content.trim();
        String normalizedImageUrl = imageUrl == null ? "" : imageUrl.trim();
        if (trimmed.isEmpty() && normalizedImageUrl.isEmpty()) {
            throw new BusinessException("回复内容或图片不能为空");
        }
        if (trimmed.length() > 500) {
            throw new BusinessException("回复内容不能超过500字");
        }
        if (normalizedImageUrl.length() > 500) {
            throw new BusinessException("图片地址过长");
        }

        String preview = trimmed.isEmpty() ? "[图片]" : trimmed;

        LocalDateTime now = LocalDateTime.now();
        int inserted = adminRepository.insertSupportMessage(conversationId, "ADMIN", trimmed, normalizedImageUrl, now);
        if (inserted <= 0) {
            throw new BusinessException("会话不存在或回复失败");
        }
        adminRepository.touchConversation(conversationId, preview, now);

        Long reporterUserId = adminRepository.findReporterUserIdByConversationId(conversationId);
        if (reporterUserId != null && reporterUserId > 0) {
            messageWebSocketNotifier.sendConversationEvent(
                    reporterUserId,
                    "SUPPORT_MESSAGE_CREATED",
                    new MessageConversationDto(
                            conversationId,
                            0L,
                            "平台客服",
                            "",
                            0L,
                            "客服工单",
                            "",
                            1,
                            preview,
                            now.toString()),
                    new MessageItemDto(
                            0L,
                            "other",
                            trimmed,
                            normalizedImageUrl,
                            now.toString(),
                            normalizedImageUrl.isEmpty() ? "TEXT" : "IMAGE",
                            null,
                            ""));
        }
    }

    @Transactional
    public void appendUserSupportMessage(Long userId, String content) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户参数无效");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("消息不能为空");
        }
        String trimmed = content.trim();
        if (trimmed.length() > 500) {
            throw new BusinessException("消息不能超过500字");
        }

        LocalDateTime now = LocalDateTime.now();
        Long conversationId = adminRepository.findActiveConversationIdByReporter(userId);
        if (conversationId == null || conversationId <= 0) {
            conversationId = adminRepository.createSupportConversationForUser(
                    userId,
                    "客服咨询",
                    "OTHER",
                    trimmed,
                    trimmed.length() > 120 ? trimmed.substring(0, 120) : trimmed,
                    "OPEN",
                    now);
            if (conversationId == null || conversationId <= 0) {
                throw new BusinessException("创建客服会话失败");
            }
        }
        adminRepository.insertSupportMessage(conversationId, "USER", trimmed, "", now);
        adminRepository.touchConversation(conversationId, trimmed, now);

        Long adminUserId = adminRepository.findUserIdByEmail(adminEmail);
        if (adminUserId != null && adminUserId > 0) {
            messageWebSocketNotifier.sendConversationEvent(
                    adminUserId,
                    "SUPPORT_MESSAGE_CREATED",
                    new MessageConversationDto(
                            conversationId,
                            userId,
                            "用户",
                            "",
                            0L,
                            "客服工单",
                            "",
                            1,
                            trimmed,
                            now.toString()),
                    new MessageItemDto(
                            0L,
                            "other",
                            trimmed,
                            "",
                            now.toString(),
                            "TEXT",
                            null,
                            ""));
        }

        messageWebSocketNotifier.sendConversationEvent(
                userId,
                "SUPPORT_MESSAGE_CREATED",
                new MessageConversationDto(
                        conversationId,
                        0L,
                        "平台客服",
                        "",
                        0L,
                        "客服工单",
                        "",
                        0,
                        trimmed,
                        now.toString()),
                new MessageItemDto(
                        0L,
                        "self",
                        trimmed,
                        "",
                        now.toString(),
                        "TEXT",
                        null,
                        ""));
    }

    public List<AdminSupportMessageDto> listUserSupportMessages(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户参数无效");
        }
        Long conversationId = adminRepository.findLatestConversationIdByReporter(userId);
        if (conversationId == null || conversationId <= 0) {
            return Collections.emptyList();
        }
        return adminRepository.listSupportMessages(conversationId);
    }

    @Transactional
    public void updateConversationStatus(Long conversationId, String status) {
        if (conversationId == null || conversationId <= 0) {
            throw new BusinessException("会话参数无效");
        }
        if (status == null || status.trim().isEmpty()) {
            throw new BusinessException("状态不能为空");
        }
        String normalized = status.trim().toUpperCase();
        if (!"OPEN".equals(normalized) && !"PROCESSING".equals(normalized) && !"RESOLVED".equals(normalized)
                && !"CLOSED".equals(normalized)) {
            throw new BusinessException("状态不合法");
        }
        int updated = adminRepository.updateSupportConversationStatus(conversationId, normalized, LocalDateTime.now());
        if (updated <= 0) {
            throw new BusinessException("会话不存在");
        }
    }

    public List<AdminNoticeDto> listNotices() {
        return adminRepository.listNotices();
    }

    @Transactional
    public void createNotice(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("公告内容不能为空");
        }
        String trimmed = content.trim();
        if (trimmed.length() > 500) {
            throw new BusinessException("公告内容不能超过500字");
        }
        adminRepository.createNotice(trimmed, LocalDateTime.now());
    }

    @Transactional
    public void deleteNotice(Long noticeId) {
        if (noticeId == null || noticeId <= 0) {
            throw new BusinessException("公告参数无效");
        }
        int deleted = adminRepository.deleteNotice(noticeId);
        if (deleted <= 0) {
            throw new BusinessException("公告不存在");
        }
    }

    private void mutateItemStatus(Long itemId, String status) {
        if (itemId == null || itemId <= 0) {
            throw new BusinessException("商品参数无效");
        }
        int updated = adminRepository.updateItemStatus(itemId, status, LocalDateTime.now());
        if (updated <= 0) {
            throw new BusinessException("商品不存在或已删除");
        }
    }

    private String toItemStatus(String status) {
        if ("PENDING_REVIEW".equals(status)) {
            return "pending";
        }
        if ("PUBLISHED".equals(status)) {
            return "online";
        }
        if ("OFF_SHELF".equals(status)) {
            return "offline";
        }
        return "offline";
    }

    private String toOrderStatus(String status) {
        if ("PENDING_PAYMENT".equals(status)) {
            return "pending-pay";
        }
        if ("PAID".equals(status)) {
            return "completed";
        }
        if ("CANCELLED".equals(status)) {
            return "cancelled";
        }
        return "cancelled";
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
