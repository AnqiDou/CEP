package com.example.cep_backend.auth.service;

import com.example.cep_backend.auth.BusinessException;
import com.example.cep_backend.auth.dto.AuthUserDto;
import com.example.cep_backend.auth.dto.LoginRequest;
import com.example.cep_backend.auth.dto.RegisterRequest;
import com.example.cep_backend.auth.dto.SendCodeRequest;
import com.example.cep_backend.auth.dto.VerifyCodeRequest;
import com.example.cep_backend.auth.model.UserRecord;
import com.example.cep_backend.auth.model.VerificationCodeRecord;
import com.example.cep_backend.auth.repository.UserRepository;
import com.example.cep_backend.auth.repository.VerificationCodeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final String PURPOSE_REGISTER = "REGISTER";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern CODE_PATTERN = Pattern.compile("^\\d{6}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,20}$");

    private final UserRepository userRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final int expireMinutes;
    private final int resendSeconds;

    public AuthService(UserRepository userRepository,
            VerificationCodeRepository verificationCodeRepository,
            EmailService emailService,
            @Value("${app.auth.code.expire-minutes}") int expireMinutes,
            @Value("${app.auth.code.resend-seconds}") int resendSeconds) {
        this.userRepository = userRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.emailService = emailService;
        this.expireMinutes = expireMinutes;
        this.resendSeconds = resendSeconds;
    }

    public void sendRegisterCode(SendCodeRequest request) {
        String email = normalizeEmail(request.email());
        validateEmail(email);
        ensureEmailNotRegistered(email);

        LocalDateTime now = LocalDateTime.now();
        Optional<LocalDateTime> latest = verificationCodeRepository.findLastCreateTime(email, PURPOSE_REGISTER);
        if (latest.isPresent()) {
            Duration duration = Duration.between(latest.get(), now);
            if (duration.getSeconds() < resendSeconds) {
                throw new BusinessException("验证码发送过于频繁，请稍后再试");
            }
        }

        String code = generateCode();
        verificationCodeRepository.saveCode(email, PURPOSE_REGISTER, code, now.plusMinutes(expireMinutes), now);
        emailService.sendRegisterCode(email, code);
    }

    public void verifyRegisterCode(VerifyCodeRequest request) {
        String email = normalizeEmail(request.email());
        String code = normalize(request.code());
        validateEmail(email);
        validateCode(code);

        VerificationCodeRecord record = findLatestRegisterCode(email);
        if (!record.code().equals(code)) {
            throw new BusinessException("验证码不正确");
        }
    }

    public AuthUserDto register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        String username = normalize(request.username());
        String code = normalize(request.code());
        String password = request.password();

        validateEmail(email);
        validateCode(code);
        validatePassword(password);
        ensureEmailNotRegistered(email);

        VerificationCodeRecord record = findLatestRegisterCode(email);
        if (!record.code().equals(code)) {
            throw new BusinessException("验证码不正确");
        }

        String passwordHash = passwordEncoder.encode(password);
        LocalDateTime now = LocalDateTime.now();
        long userId = userRepository.createUser(email, username, passwordHash, now);
        verificationCodeRepository.markUsed(record.id());

        return new AuthUserDto(userId, email, username);
    }

    public AuthUserDto login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        String password = request.password();

        validateEmail(email);
        if (password == null || password.isEmpty()) {
            throw new BusinessException("请输入密码");
        }

        UserRecord user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("邮箱或密码错误"));

        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw new BusinessException("邮箱或密码错误");
        }

        userRepository.updateLastLoginAt(user.id(), LocalDateTime.now());
        return new AuthUserDto(user.id(), user.email(), user.username());
    }

    private VerificationCodeRecord findLatestRegisterCode(String email) {
        return verificationCodeRepository.findLatestUnUsedCode(email, PURPOSE_REGISTER, LocalDateTime.now())
                .orElseThrow(() -> new BusinessException("验证码不存在或已过期"));
    }

    private void ensureEmailNotRegistered(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException("该邮箱已注册");
        }
    }

    private void validateEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException("邮箱格式不正确");
        }
    }

    private void validateCode(String code) {
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new BusinessException("验证码格式不正确，请输入6位数字");
        }
    }

    private void validatePassword(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException("密码需为8-20位，且同时包含字母和数字");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeEmail(String email) {
        return normalize(email).toLowerCase();
    }

    private String generateCode() {
        int value = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(value);
    }
}
