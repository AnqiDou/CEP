package com.example.cep_backend.admin.controller;

import com.example.cep_backend.admin.dto.AdminDashboardDto;
import com.example.cep_backend.admin.dto.AdminItemDto;
import com.example.cep_backend.admin.dto.AdminNoticeCreateRequest;
import com.example.cep_backend.admin.dto.AdminNoticeDto;
import com.example.cep_backend.admin.dto.AdminOrderDto;
import com.example.cep_backend.admin.dto.AdminSupportConversationDto;
import com.example.cep_backend.admin.dto.AdminSupportReplyRequest;
import com.example.cep_backend.admin.dto.AdminUserDto;
import com.example.cep_backend.admin.dto.AdminUserStatusRequest;
import com.example.cep_backend.admin.service.AdminService;
import com.example.cep_backend.auth.dto.AuthUserDto;
import com.example.cep_backend.auth.service.AuthService;
import com.example.cep_backend.common.api.ApiResponse;
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
            @RequestParam(required = false) String keyword) {
        ensureAdmin(authorization);
        return ApiResponse.ok("获取成功", adminService.listUsers(keyword));
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
            @RequestParam(required = false) String status) {
        ensureAdmin(authorization);
        return ApiResponse.ok("获取成功", adminService.listItems(keyword, status));
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
            @RequestParam(required = false) String status) {
        ensureAdmin(authorization);
        return ApiResponse.ok("获取成功", adminService.listOrders(keyword, status));
    }

    @PostMapping("/orders/{orderNo}/handle-abnormal")
    public ApiResponse<Void> handleOrderAbnormal(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String orderNo) {
        ensureAdmin(authorization);
        adminService.handleOrderAbnormal(orderNo);
        return ApiResponse.ok("处理成功");
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
        adminService.replyConversation(conversationId, request == null ? null : request.content());
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
