package cep_backend.controller;
import cep_backend.dto.AuthUserDto;
import cep_backend.dto.RegisterRequest;
import cep_backend.dto.ResetPasswordRequest;
import cep_backend.service.AuthService;
import cep_backend.common.result.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthAccountController {
    private final AuthService authService;

    public AuthAccountController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthUserDto> register(@RequestBody RegisterRequest request) {
        AuthUserDto user = authService.register(request);
        return ApiResponse.ok("注册成功", user);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok("密码重置成功");
    }
}
