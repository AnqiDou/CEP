package cep_backend.controller;
import cep_backend.dto.AdminDashboardDto;
import cep_backend.dto.AdminItemDto;
import cep_backend.dto.AdminNoticeCreateRequest;
import cep_backend.dto.AdminNoticeDto;
import cep_backend.dto.AdminOrderDto;
import cep_backend.dto.AdminOrderUpdateRequest;
import cep_backend.dto.AdminSupportConversationDto;
import cep_backend.dto.AdminSupportMessageDto;
import cep_backend.dto.AdminSupportReplyRequest;
import cep_backend.dto.AdminSupportStatusRequest;
import cep_backend.dto.AdminUserCreditScoreRequest;
import cep_backend.dto.AdminUserDto;
import cep_backend.dto.AdminUserStatusRequest;
import cep_backend.service.AdminService;
import cep_backend.dto.AuthUserDto;
import cep_backend.service.AuthService;
import cep_backend.common.result.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AuthService authService;
    private final AdminService adminService;

    public AdminController(AuthService authService, AdminService adminService) {
        this.authService = authService;
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardDto> dashboard(
            @RequestHeader("Authorization") String authorization) {
        ensureAdmin(authorization);
        return ApiResponse.ok("获取成功", adminService.dashboard());
    }

    @GetMapping("/users")
    public ApiResponse<List<AdminUserDto>> users(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email) {
        ensureAdmin(authorization);
        return ApiResponse.ok("获取成功", adminService.listUsers(keyword, username, phone, email));
    }

    @PatchMapping("/users/{userId}/status")
    public ApiResponse<Void> updateUserStatus(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long userId,
            @RequestBody AdminUserStatusRequest request) {
        ensureAdmin(authorization);
        adminService.updateUserStatus(userId, request == null ? null : request.disabled());
        return ApiResponse.ok("更新成功");
    }

    @PatchMapping("/users/{userId}/credit-score")
    public ApiResponse<Void> updateUserCreditScore(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long userId,
            @RequestBody AdminUserCreditScoreRequest request) {
        ensureAdmin(authorization);
        adminService.updateUserCreditScore(
                userId,
                request == null ? null : request.role(),
                request == null ? null : request.creditScore());
        return ApiResponse.ok("更新成功");
    }

    @DeleteMapping("/users/{userId}")
    public ApiResponse<Void> deleteUser(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long userId) {
        ensureAdmin(authorization);
        adminService.deleteUser(userId);
        return ApiResponse.ok("删除成功");
    }

    @GetMapping("/items")
    public ApiResponse<List<AdminItemDto>> items(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String price,
            @RequestParam(required = false) String publisher,
            @RequestParam(required = false) String status) {
        ensureAdmin(authorization);
        return ApiResponse.ok("获取成功",
                adminService.listItems(keyword, title, category, price, publisher, status));
    }

    @PostMapping("/items/{itemId}/approve")
    public ApiResponse<Void> approveItem(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long itemId) {
        ensureAdmin(authorization);
        adminService.approveItem(itemId);
        return ApiResponse.ok("审核成功");
    }

    @PostMapping("/items/{itemId}/offline")
    public ApiResponse<Void> forceOffline(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long itemId) {
        ensureAdmin(authorization);
        adminService.forceOffline(itemId);
        return ApiResponse.ok("下架成功");
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<Void> deleteItem(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long itemId) {
        ensureAdmin(authorization);
        adminService.deleteItem(itemId);
        return ApiResponse.ok("删除成功");
    }

    @GetMapping("/orders")
    public ApiResponse<List<AdminOrderDto>> orders(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String buyer,
            @RequestParam(required = false) String seller,
            @RequestParam(required = false) String itemTitle,
            @RequestParam(required = false) String status) {
        ensureAdmin(authorization);
        return ApiResponse.ok("获取成功", adminService.listOrders(keyword, orderNo, buyer, seller, itemTitle, status));
    }

    @PostMapping("/orders/{orderNo}/handle-abnormal")
    public ApiResponse<Void> handleOrderAbnormal(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String orderNo) {
        ensureAdmin(authorization);
        adminService.handleOrderAbnormal(orderNo);
        return ApiResponse.ok("处理成功");
    }

    @PatchMapping("/orders/{orderNo}")
    public ApiResponse<Void> updateOrder(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String orderNo,
            @RequestBody AdminOrderUpdateRequest request) {
        ensureAdmin(authorization);
        adminService.updateOrder(
                orderNo,
                request == null ? null : request.status(),
                request == null ? null : request.refundStatus());
        return ApiResponse.ok("更新成功");
    }

    @GetMapping("/support/conversations")
    public ApiResponse<List<AdminSupportConversationDto>> conversations(
            @RequestHeader("Authorization") String authorization) {
        ensureAdmin(authorization);
        return ApiResponse.ok("获取成功", adminService.listConversations());
    }

    @PostMapping("/support/conversations/{conversationId}/messages")
    public ApiResponse<Void> replyConversation(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long conversationId,
            @RequestBody AdminSupportReplyRequest request) {
        ensureAdmin(authorization);
        adminService.replyConversation(
                conversationId,
                request == null ? null : request.content(),
                request == null ? null : request.imageUrl());
        return ApiResponse.ok("发送成功");
    }

    @PatchMapping("/support/conversations/{conversationId}/status")
    public ApiResponse<Void> updateConversationStatus(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long conversationId,
            @RequestBody AdminSupportStatusRequest request) {
        ensureAdmin(authorization);
        adminService.updateConversationStatus(conversationId, request == null ? null : request.status());
        return ApiResponse.ok("更新成功");
    }

    @GetMapping("/support/me/messages")
    public ApiResponse<List<AdminSupportMessageDto>> mySupportMessages(
            @RequestHeader("Authorization") String authorization) {
        AuthUserDto currentUser = authService.currentUser(authorization, true);
        return ApiResponse.ok("获取成功", adminService.listUserSupportMessages(currentUser.userId()));
    }

    @PostMapping("/support/me/messages")
    public ApiResponse<Void> sendMySupportMessage(
            @RequestHeader("Authorization") String authorization,
            @RequestBody AdminSupportReplyRequest request) {
        AuthUserDto currentUser = authService.currentUser(authorization, true);
        adminService.appendUserSupportMessage(
                currentUser.userId(),
                request == null ? null : request.content(),
                request == null ? null : request.orderId());
        return ApiResponse.ok("发送成功");
    }

    @GetMapping("/notices")
    public ApiResponse<List<AdminNoticeDto>> notices(
            @RequestHeader("Authorization") String authorization) {
        ensureAdmin(authorization);
        return ApiResponse.ok("获取成功", adminService.listNotices());
    }

    @PostMapping("/notices")
    public ApiResponse<Void> createNotice(
            @RequestHeader("Authorization") String authorization,
            @RequestBody AdminNoticeCreateRequest request) {
        ensureAdmin(authorization);
        adminService.createNotice(request == null ? null : request.content());
        return ApiResponse.ok("发布成功");
    }

    @DeleteMapping("/notices/{noticeId}")
    public ApiResponse<Void> deleteNotice(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long noticeId) {
        ensureAdmin(authorization);
        adminService.deleteNotice(noticeId);
        return ApiResponse.ok("删除成功");
    }

    private void ensureAdmin(String authorization) {
        AuthUserDto currentUser = authService.currentUser(authorization);
        adminService.ensureAdmin(currentUser);
    }
}
