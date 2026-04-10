package cep_backend.service;
import cep_backend.dto.AdminDashboardDto;
import cep_backend.dto.AdminItemDto;
import cep_backend.dto.AdminNoticeDto;
import cep_backend.dto.AdminOrderDto;
import cep_backend.dto.AdminOrderStateStatDto;
import cep_backend.dto.AdminSupportConversationDto;
import cep_backend.dto.AdminSupportMessageDto;
import cep_backend.dto.AdminUserDto;
import cep_backend.mapper.AdminRepository;
import cep_backend.common.exception.BusinessException;
import cep_backend.dto.AuthUserDto;
import cep_backend.dto.MessageConversationDto;
import cep_backend.dto.MessageItemDto;
import cep_backend.dto.MessageSendRequest;
import cep_backend.mapper.MessageRepository;
import cep_backend.service.MessageService;
import cep_backend.util.ws.MessageWebSocketNotifier;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    public static final BigDecimal PUBLISH_CREDIT_THRESHOLD = new BigDecimal("85.0");

    private final AdminRepository adminRepository;
    private final MessageRepository messageRepository;
    private final MessageService messageService;
    private final String adminEmail;
    private final MessageWebSocketNotifier messageWebSocketNotifier;

    public AdminService(AdminRepository adminRepository,
            MessageRepository messageRepository,
            MessageService messageService,
            MessageWebSocketNotifier messageWebSocketNotifier,
            @Value("${app.admin.email:3299166215@qq.com}") String adminEmail) {
        this.adminRepository = adminRepository;
        this.messageRepository = messageRepository;
        this.messageService = messageService;
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
        int pendingConfirmCount = rawStats.getOrDefault("PENDING_CONFIRMATION", 0);
        int completedCount = rawStats.getOrDefault("COMPLETED", 0);
        int cancelledCount = rawStats.getOrDefault("CANCELLED", 0);
        int total = Math.max(pendingPayCount + pendingConfirmCount + completedCount + cancelledCount, 1);

        List<AdminOrderStateStatDto> stats = List.of(
                new AdminOrderStateStatDto("待付款", pendingPayCount, pendingPayCount * 100 / total),
                new AdminOrderStateStatDto("待确认", pendingConfirmCount, pendingConfirmCount * 100 / total),
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
    public void updateUserCreditScore(Long userId, String role, BigDecimal creditScore) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户参数无效");
        }
        String normalizedRole = role == null ? "" : role.trim().toUpperCase();
        if (!"SELLER".equals(normalizedRole) && !"BUYER".equals(normalizedRole)) {
            throw new BusinessException("信用角色参数无效");
        }
        if (creditScore == null) {
            throw new BusinessException("信用分不能为空");
        }

        BigDecimal normalized = creditScore.setScale(1, RoundingMode.HALF_UP);

        int updated = adminRepository.upsertUserCreditScore(userId, normalizedRole, normalized, LocalDateTime.now());
        if (updated <= 0) {
            throw new BusinessException("更新用户信用分失败");
        }
    }

    public BigDecimal resolveUserCreditScoreOrDefault(Long userId, String role) {
        if (userId == null || userId <= 0) {
            return new BigDecimal("100.0");
        }
        String normalizedRole = role == null ? "" : role.trim().toUpperCase();
        if (!"SELLER".equals(normalizedRole) && !"BUYER".equals(normalizedRole)) {
            normalizedRole = "SELLER";
        }
        BigDecimal score = adminRepository.findUserCreditScore(userId, normalizedRole);
        return score == null ? new BigDecimal("100.0") : score;
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
                        toOrderStatus(order.status()),
                        order.refundStatus() == null ? "none" : order.refundStatus().trim().toLowerCase()))
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

    @Transactional
    public void updateOrder(String orderNo, String status, String refundStatus) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            throw new BusinessException("订单号不能为空");
        }
        String normalizedOrderNo = orderNo.trim();
        LocalDateTime now = LocalDateTime.now();
        AdminRepository.TradeOrderNotifyRecord before = adminRepository
                .findTradeOrderNotifyRecordByOrderNo(normalizedOrderNo);

        String normalizedStatus = status == null ? "" : status.trim().toUpperCase();
        if (!normalizedStatus.isEmpty()) {
            if (!"PENDING_PAYMENT".equals(normalizedStatus)
                    && !"PENDING_CONFIRMATION".equals(normalizedStatus)
                    && !"COMPLETED".equals(normalizedStatus)
                    && !"CANCELLED".equals(normalizedStatus)) {
                throw new BusinessException("订单状态不合法");
            }
            int statusUpdated = adminRepository.updateOrderStatusByOrderNo(normalizedOrderNo, normalizedStatus, now);
            if (statusUpdated <= 0) {
                throw new BusinessException("订单不存在");
            }
        }

        String normalizedRefund = refundStatus == null ? "" : refundStatus.trim().toUpperCase();
        if (!normalizedRefund.isEmpty()) {
            if (!"NONE".equals(normalizedRefund)
                    && !"APPLIED".equals(normalizedRefund)
                    && !"APPROVED".equals(normalizedRefund)
                    && !"REJECTED".equals(normalizedRefund)) {
                throw new BusinessException("退款状态不合法");
            }
            int refundUpdated = adminRepository.updateOrderRefundStatusByOrderNo(normalizedOrderNo, normalizedRefund,
                    now);
            if (refundUpdated <= 0) {
                throw new BusinessException("订单不存在");
            }
        }

        AdminRepository.TradeOrderNotifyRecord after = adminRepository
                .findTradeOrderNotifyRecordByOrderNo(normalizedOrderNo);
        notifyTradeOrderUpdatedByAdmin(before, after, now);
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
                            false,
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
    public void appendUserSupportMessage(Long userId, String content, Long orderId) {
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
        String orderContext = "";
        if (orderId != null && orderId > 0) {
            String orderNo = adminRepository.findOrderNoByOrderIdAndUserId(orderId, userId);
            if (orderNo != null && !orderNo.trim().isEmpty()) {
                orderContext = "\n[订单号:" + orderNo.trim() + "]";
            } else {
                orderContext = "\n[订单号#" + orderId + "]";
            }
        }
        String finalMessage = trimmed + orderContext;
        Long conversationId = adminRepository.findActiveConversationIdByReporter(userId);
        if (conversationId == null || conversationId <= 0) {
            conversationId = adminRepository.createSupportConversationForUser(
                    userId,
                    "客服咨询",
                    "OTHER",
                    finalMessage,
                    finalMessage.length() > 120 ? finalMessage.substring(0, 120) : finalMessage,
                    "OPEN",
                    now);
            if (conversationId == null || conversationId <= 0) {
                throw new BusinessException("创建客服会话失败");
            }
        }
        adminRepository.insertSupportMessage(conversationId, "USER", finalMessage, "", now);
        adminRepository.touchConversation(conversationId, finalMessage, now);

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
                            false,
                            1,
                            finalMessage,
                            now.toString()),
                    new MessageItemDto(
                            0L,
                            "other",
                            finalMessage,
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
                        false,
                        0,
                        finalMessage,
                        now.toString()),
                new MessageItemDto(
                        0L,
                        "self",
                        finalMessage,
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
        if ("PENDING_CONFIRMATION".equals(status)) {
            return "pending-confirm";
        }
        if ("COMPLETED".equals(status)) {
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

    private void notifyTradeOrderUpdatedByAdmin(
            AdminRepository.TradeOrderNotifyRecord before,
            AdminRepository.TradeOrderNotifyRecord after,
            LocalDateTime now) {
        if (after == null) {
            return;
        }
        if (after.orderId() == null || after.orderId() <= 0
                || after.itemId() == null || after.itemId() <= 0
                || after.buyerUserId() == null || after.buyerUserId() <= 0
                || after.sellerUserId() == null || after.sellerUserId() <= 0) {
            return;
        }

        String beforeStatus = normalizeUpper(before == null ? null : before.status());
        String afterStatus = normalizeUpper(after.status());
        String beforeRefundStatus = normalizeUpper(before == null ? null : before.refundStatus());
        String afterRefundStatus = normalizeUpper(after.refundStatus());

        boolean statusChanged = !beforeStatus.equals(afterStatus);
        boolean refundChanged = !beforeRefundStatus.equals(afterRefundStatus);
        if (!statusChanged && !refundChanged) {
            return;
        }

        Long conversationId = resolveOrCreateTradeConversationId(after.itemId(), after.buyerUserId(),
                after.sellerUserId(), now);
        if (conversationId == null || conversationId <= 0) {
            return;
        }

        String content = buildAdminOrderUpdateContent(afterStatus, afterRefundStatus, statusChanged, refundChanged);
        String reminderText = buildTradeReminderText(
                "ADMIN_ORDER_UPDATED",
                after.orderId(),
                after.itemTitle(),
                content,
                "查看订单",
                "");

        Long senderUserId = after.sellerUserId();
        try {
            MessageService.MessageDispatchResult result = messageService.sendMessage(
                    senderUserId,
                    new MessageSendRequest(conversationId, reminderText, ""));
            messageWebSocketNotifier.notifyMessageCreated(result);
        } catch (RuntimeException ex) {
            // 管理员改状态不应被通知失败阻断
        }
    }

    private Long resolveOrCreateTradeConversationId(Long itemId, Long buyerUserId, Long sellerUserId,
            LocalDateTime now) {
        Long conversationId = messageRepository.findConversationIdByItemAndPair(itemId, buyerUserId, sellerUserId);
        if (conversationId != null && conversationId > 0) {
            return conversationId;
        }
        try {
            conversationId = messageRepository.createConversation(itemId, buyerUserId, sellerUserId, now);
        } catch (DataAccessException ex) {
            conversationId = messageRepository.findConversationIdByItemAndPair(itemId, buyerUserId, sellerUserId);
        }
        return conversationId;
    }

    private String buildAdminOrderUpdateContent(
            String tradeStatus,
            String refundStatus,
            boolean statusChanged,
            boolean refundChanged) {
        if (statusChanged && refundChanged) {
            return "管理员已更新订单状态为「" + toOrderStatusTextZh(tradeStatus)
                    + "」，退款状态为「" + toRefundStatusTextZh(refundStatus) + "」，请留意最新进展。";
        }
        if (statusChanged) {
            return "管理员已将订单状态更新为「" + toOrderStatusTextZh(tradeStatus) + "」，请留意最新进展。";
        }
        return "管理员已将退款状态更新为「" + toRefundStatusTextZh(refundStatus) + "」，请留意最新进展。";
    }

    private String buildTradeReminderText(
            String type,
            Long orderId,
            String itemTitle,
            String content,
            String actionText,
            String targetMenu) {
        String safeType = jsonEscape(type == null ? "TRADE_REMINDER" : type.trim());
        String safeItemTitle = jsonEscape(itemTitle == null ? "" : itemTitle.trim());
        String safeContent = jsonEscape(content == null ? "交易状态已更新，请及时处理。" : content.trim());
        String safeActionText = jsonEscape(actionText == null ? "去查看" : actionText.trim());
        String safeTargetMenu = jsonEscape(targetMenu == null ? "" : targetMenu.trim());
        String orderPart = orderId != null && orderId > 0 ? String.valueOf(orderId) : "null";
        return "[TRADE_REMINDER]{\"type\":\"" + safeType
                + "\",\"orderId\":" + orderPart
                + ",\"itemTitle\":\"" + safeItemTitle
                + "\",\"content\":\"" + safeContent
                + "\",\"actionText\":\"" + safeActionText
                + "\",\"targetMenu\":\"" + safeTargetMenu + "\"}";
    }

    private String jsonEscape(String raw) {
        if (raw == null) {
            return "";
        }
        return raw
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private String normalizeUpper(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase();
    }

    private String toOrderStatusTextZh(String status) {
        return switch (normalizeUpper(status)) {
            case "PENDING_PAYMENT" -> "待付款";
            case "PENDING_CONFIRMATION" -> "待确认";
            case "COMPLETED" -> "已完成";
            case "CANCELLED" -> "已取消";
            default -> "处理中";
        };
    }

    private String toRefundStatusTextZh(String status) {
        return switch (normalizeUpper(status)) {
            case "NONE" -> "无退款";
            case "APPLIED" -> "退款申请中";
            case "APPROVED" -> "已退款";
            case "REJECTED" -> "退款已拒绝";
            default -> "处理中";
        };
    }
}
