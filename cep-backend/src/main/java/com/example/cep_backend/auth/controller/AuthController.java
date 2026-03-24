package com.example.cep_backend.auth.controller;

import com.example.cep_backend.common.api.ApiResponse;
import com.example.cep_backend.auth.dto.AuthSessionDto;
import com.example.cep_backend.auth.dto.AuthUserDto;
import com.example.cep_backend.auth.dto.LoginRequest;
import com.example.cep_backend.auth.dto.RefreshTokenRequest;
import com.example.cep_backend.auth.dto.RegisterRequest;
import com.example.cep_backend.auth.dto.ResetPasswordRequest;
import com.example.cep_backend.auth.dto.SendCodeRequest;
import com.example.cep_backend.auth.dto.VerifyCodeRequest;
import com.example.cep_backend.auth.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/send-register-code")
    public ApiResponse<Void> sendRegisterCode(@RequestBody SendCodeRequest request) {
        authService.sendRegisterCode(request);
        return ApiResponse.ok("验证码发送成功");
    }

    @PostMapping("/verify-register-code")
    public ApiResponse<Void> verifyRegisterCode(@RequestBody VerifyCodeRequest request) {
        authService.verifyRegisterCode(request);
        return ApiResponse.ok("验证码校验通过");
    }

    @PostMapping("/send-reset-password-code")
    public ApiResponse<Void> sendResetPasswordCode(@RequestBody SendCodeRequest request) {
        authService.sendResetPasswordCode(request);
        return ApiResponse.ok("验证码发送成功");
    }

    @PostMapping("/verify-reset-password-code")
    public ApiResponse<Void> verifyResetPasswordCode(@RequestBody VerifyCodeRequest request) {
        authService.verifyResetPasswordCode(request);
        return ApiResponse.ok("验证码校验通过");
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok("密码重置成功");
    }

    @PostMapping("/register")
    public ApiResponse<AuthUserDto> register(@RequestBody RegisterRequest request) {
        AuthUserDto user = authService.register(request);
        return ApiResponse.ok("注册成功", user);
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
