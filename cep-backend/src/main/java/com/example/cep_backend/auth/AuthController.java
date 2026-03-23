package com.example.cep_backend.auth;

import com.example.cep_backend.auth.dto.AuthUserDto;
import com.example.cep_backend.auth.dto.LoginRequest;
import com.example.cep_backend.auth.dto.RegisterRequest;
import com.example.cep_backend.auth.dto.SendCodeRequest;
import com.example.cep_backend.auth.dto.VerifyCodeRequest;
import com.example.cep_backend.auth.service.AuthService;
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

    @PostMapping("/register")
    public ApiResponse<AuthUserDto> register(@RequestBody RegisterRequest request) {
        AuthUserDto user = authService.register(request);
        return ApiResponse.ok("注册成功", user);
    }

    @PostMapping("/login")
    public ApiResponse<AuthUserDto> login(@RequestBody LoginRequest request) {
        AuthUserDto user = authService.login(request);
        return ApiResponse.ok("登录成功", user);
    }
}
