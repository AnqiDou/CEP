package com.example.cep_backend.auth.session.controller;

import com.example.cep_backend.auth.dto.AuthSessionDto;
import com.example.cep_backend.auth.dto.AuthUserDto;
import com.example.cep_backend.auth.dto.LoginRequest;
import com.example.cep_backend.auth.dto.RefreshTokenRequest;
import com.example.cep_backend.auth.service.AuthService;
import com.example.cep_backend.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthSessionController {
    private final AuthService authService;

    public AuthSessionController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthSessionDto> login(@RequestBody LoginRequest request) {
        AuthSessionDto user = authService.login(request);
        return ApiResponse.ok("登录成功", user);
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthSessionDto> refresh(@RequestBody RefreshTokenRequest request) {
        AuthSessionDto session = authService.refreshSession(request);
        return ApiResponse.ok("刷新成功", session);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ApiResponse.ok("退出成功");
    }

    @GetMapping("/me")
    public ApiResponse<AuthUserDto> currentUser(@RequestHeader("Authorization") String authorization) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("获取成功", user);
    }
}
