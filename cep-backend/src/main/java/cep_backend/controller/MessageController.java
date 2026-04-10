package cep_backend.controller;
import cep_backend.dto.AuthUserDto;
import cep_backend.service.AuthService;
import cep_backend.common.result.ApiResponse;
import cep_backend.dto.MessageConversationDto;
import cep_backend.dto.MessageCreateConversationRequest;
import cep_backend.dto.MessageItemDto;
import cep_backend.dto.MessageNotificationDto;
import cep_backend.dto.MessageNotificationUnreadDto;
import cep_backend.service.MessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService messageService;
    private final AuthService authService;

    public MessageController(MessageService messageService, AuthService authService) {
        this.messageService = messageService;
        this.authService = authService;
    }

    @GetMapping("/conversations")
    public ApiResponse<List<MessageConversationDto>> getConversations(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "all") String filter) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("获取成功", messageService.getConversations(user.userId(), filter));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<List<MessageItemDto>> getConversationMessages(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long conversationId) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("获取成功", messageService.getConversationMessages(user.userId(), conversationId));
    }

    @PostMapping("/conversations/direct")
    public ApiResponse<MessageConversationDto> createOrGetDirectConversation(
            @RequestHeader("Authorization") String authorization,
            @RequestBody MessageCreateConversationRequest request) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("获取成功", messageService.createOrGetConversation(user.userId(), request));
    }

    @PostMapping("/conversations/{conversationId}/read")
    public ApiResponse<Void> markConversationRead(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long conversationId) {
        AuthUserDto user = authService.currentUser(authorization);
        messageService.markConversationRead(user.userId(), conversationId);
        return ApiResponse.ok("已标记已读", null);
    }

    @GetMapping("/notifications")
    public ApiResponse<List<MessageNotificationDto>> getNotifications(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Integer limit) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("获取成功", messageService.getNotifications(user.userId(), limit));
    }

    @GetMapping("/notifications/unread-count")
    public ApiResponse<MessageNotificationUnreadDto> getNotificationUnreadCount(
            @RequestHeader("Authorization") String authorization) {
        AuthUserDto user = authService.currentUser(authorization);
        int unread = messageService.getNotificationUnreadCount(user.userId());
        return ApiResponse.ok("获取成功", new MessageNotificationUnreadDto(unread));
    }

    @PostMapping("/notifications/{notificationId}/read")
    public ApiResponse<Void> markNotificationRead(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long notificationId) {
        AuthUserDto user = authService.currentUser(authorization);
        messageService.markNotificationRead(user.userId(), notificationId);
        return ApiResponse.ok("已标记已读", null);
    }

    @PostMapping("/notifications/read-all")
    public ApiResponse<Void> markAllNotificationsRead(
            @RequestHeader("Authorization") String authorization) {
        AuthUserDto user = authService.currentUser(authorization);
        messageService.markAllNotificationsRead(user.userId());
        return ApiResponse.ok("已全部标记已读", null);
    }
}
