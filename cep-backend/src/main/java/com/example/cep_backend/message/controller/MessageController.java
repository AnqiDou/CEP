package com.example.cep_backend.message.controller;

import com.example.cep_backend.auth.dto.AuthUserDto;
import com.example.cep_backend.auth.service.AuthService;
import com.example.cep_backend.common.api.ApiResponse;
import com.example.cep_backend.message.dto.MessageConversationDto;
import com.example.cep_backend.message.dto.MessageCreateConversationRequest;
import com.example.cep_backend.message.dto.MessageItemDto;
import com.example.cep_backend.message.service.MessageService;
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
}
