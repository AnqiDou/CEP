package cep_backend.controller;
import cep_backend.dto.SendCodeRequest;
import cep_backend.dto.VerifyCodeRequest;
import cep_backend.service.AuthService;
import cep_backend.common.result.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthVerificationController {
    private final AuthService authService;

    public AuthVerificationController(AuthService authService) {
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
}
