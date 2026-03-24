package com.example.cep_backend.auth.service;

import com.example.cep_backend.auth.BusinessException;
import com.example.cep_backend.auth.UnauthorizedException;
import com.example.cep_backend.auth.dto.AuthSessionDto;
import com.example.cep_backend.auth.dto.AuthUserDto;
import com.example.cep_backend.auth.dto.LoginRequest;
import com.example.cep_backend.auth.dto.RefreshTokenRequest;
import com.example.cep_backend.auth.dto.RegisterRequest;
import com.example.cep_backend.auth.dto.ResetPasswordRequest;
import com.example.cep_backend.auth.dto.SendCodeRequest;
import com.example.cep_backend.auth.dto.VerifyCodeRequest;
import com.example.cep_backend.auth.model.AuthSessionRecord;
import com.example.cep_backend.auth.model.UserRecord;
import com.example.cep_backend.auth.model.VerificationCodeRecord;
import com.example.cep_backend.auth.repository.AuthSessionRepository;
import com.example.cep_backend.auth.repository.UserRepository;
import com.example.cep_backend.auth.repository.VerificationCodeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final String PURPOSE_REGISTER = "REGISTER";
    private static final String PURPOSE_RESET_PASSWORD = "RESET_PASSWORD";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern CODE_PATTERN = Pattern.compile("^\\d{6}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,20}$");
    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final AuthSessionRepository authSessionRepository;
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();
    private final int expireMinutes;
    private final int resendSeconds;
    private final int accessTokenExpireMinutes;
    private final int refreshTokenExpireDays;

    public AuthService(UserRepository userRepository,
            VerificationCodeRepository verificationCodeRepository,
            AuthSessionRepository authSessionRepository,
            EmailService emailService,
            @Value("${app.auth.code.expire-minutes}") int expireMinutes,
            @Value("${app.auth.code.resend-seconds}") int resendSeconds,
            @Value("${app.auth.token.access-expire-minutes}") int accessTokenExpireMinutes,
            @Value("${app.auth.token.refresh-expire-days}") int refreshTokenExpireDays) {
        this.userRepository = userRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.authSessionRepository = authSessionRepository;
        this.emailService = emailService;
        this.expireMinutes = expireMinutes;
        this.resendSeconds = resendSeconds;
        this.accessTokenExpireMinutes = accessTokenExpireMinutes;
        this.refreshTokenExpireDays = refreshTokenExpireDays;
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

    public void sendResetPasswordCode(SendCodeRequest request) {
        String email = normalizeEmail(request.email());
        validateEmail(email);
        ensureEmailRegistered(email);

        LocalDateTime now = LocalDateTime.now();
        Optional<LocalDateTime> latest = verificationCodeRepository.findLastCreateTime(email, PURPOSE_RESET_PASSWORD);
        if (latest.isPresent()) {
            Duration duration = Duration.between(latest.get(), now);
            if (duration.getSeconds() < resendSeconds) {
                throw new BusinessException("验证码发送过于频繁，请稍后再试");
            }
        }

        String code = generateCode();
        verificationCodeRepository.saveCode(email, PURPOSE_RESET_PASSWORD, code, now.plusMinutes(expireMinutes), now);
        emailService.sendResetPasswordCode(email, code);
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

    public void verifyResetPasswordCode(VerifyCodeRequest request) {
        String email = normalizeEmail(request.email());
        String code = normalize(request.code());
        validateEmail(email);
        validateCode(code);
        ensureEmailRegistered(email);

        VerificationCodeRecord record = findLatestResetPasswordCode(email);
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

    public void resetPassword(ResetPasswordRequest request) {
        String email = normalizeEmail(request.email());
        String code = normalize(request.code());
        String password = request.password();

        validateEmail(email);
        validateCode(code);
        validatePassword(password);
        ensureEmailRegistered(email);

        VerificationCodeRecord record = findLatestResetPasswordCode(email);
        if (!record.code().equals(code)) {
            throw new BusinessException("验证码不正确");
        }

        String passwordHash = passwordEncoder.encode(password);
        LocalDateTime now = LocalDateTime.now();
        userRepository.updatePasswordByEmail(email, passwordHash, now);
        verificationCodeRepository.markUsed(record.id());
    }

    public AuthSessionDto login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        String password = request.password();

        validateEmail(email);
        if (password == null || password.isEmpty()) {
            throw new BusinessException("请输入密码");
        }

        UserRecord user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("该邮箱尚未注册"));

        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw new BusinessException("密码错误");
        }

        LocalDateTime now = LocalDateTime.now();
        userRepository.updateLastLoginAt(user.id(), now);
        return issueSession(user.id(), user.email(), user.username(), now);
    }

    public AuthSessionDto refreshSession(RefreshTokenRequest request) {
        String refreshToken = normalize(request.refreshToken());
        if (refreshToken.isEmpty()) {
            throw new UnauthorizedException("刷新令牌无效，请重新登录");
        }

        String refreshTokenHash = hashToken(refreshToken);
        AuthSessionRecord session = authSessionRepository.findByRefreshTokenHash(refreshTokenHash)
                .orElseThrow(() -> new UnauthorizedException("登录状态已过期，请重新登录"));

        LocalDateTime now = LocalDateTime.now();
        if (session.revoked() || !session.refreshExpiresAt().isAfter(now)) {
            if (!session.revoked()) {
                authSessionRepository.revokeSession(session.id(), now);
            }
            throw new UnauthorizedException("登录状态已过期，请重新登录");
        }

        TokenIssue tokenIssue = createTokenIssue(now);
        authSessionRepository.rotateSession(
                session.id(),
                hashToken(tokenIssue.refreshToken()),
                hashToken(tokenIssue.accessToken()),
                tokenIssue.refreshExpiresAt(),
                tokenIssue.accessExpiresAt(),
                now);

        return new AuthSessionDto(
                session.userId(),
                session.email(),
                session.username(),
                tokenIssue.accessToken(),
                tokenIssue.refreshToken(),
                Duration.between(now, tokenIssue.accessExpiresAt()).toSeconds(),
                TOKEN_TYPE);
    }

    public AuthUserDto currentUser(String authorizationHeader) {
        String accessToken = parseBearerToken(authorizationHeader);
        String accessTokenHash = hashToken(accessToken);
        AuthSessionRecord session = authSessionRepository.findByAccessTokenHash(accessTokenHash)
                .orElseThrow(() -> new UnauthorizedException("登录状态已失效，请重新登录"));

        LocalDateTime now = LocalDateTime.now();
        if (session.revoked() || !session.accessExpiresAt().isAfter(now)) {
            throw new UnauthorizedException("登录状态已失效，请重新登录");
        }

        return new AuthUserDto(session.userId(), session.email(), session.username());
    }

    public void logout(RefreshTokenRequest request) {
        String refreshToken = normalize(request.refreshToken());
        if (refreshToken.isEmpty()) {
            return;
        }

        authSessionRepository.findByRefreshTokenHash(hashToken(refreshToken))
                .ifPresent(session -> authSessionRepository.revokeSession(session.id(), LocalDateTime.now()));
    }

    private VerificationCodeRecord findLatestRegisterCode(String email) {
        return verificationCodeRepository.findLatestUnUsedCode(email, PURPOSE_REGISTER, LocalDateTime.now())
                .orElseThrow(() -> new BusinessException("验证码不存在或已过期"));
    }

    private VerificationCodeRecord findLatestResetPasswordCode(String email) {
        return verificationCodeRepository.findLatestUnUsedCode(email, PURPOSE_RESET_PASSWORD, LocalDateTime.now())
                .orElseThrow(() -> new BusinessException("验证码不存在或已过期"));
    }

    private void ensureEmailNotRegistered(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException("该邮箱已注册");
        }
    }

    private void ensureEmailRegistered(String email) {
        if (userRepository.findByEmail(email).isEmpty()) {
            throw new BusinessException("该邮箱尚未注册");
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

    private AuthSessionDto issueSession(long userId, String email, String username, LocalDateTime now) {
        TokenIssue tokenIssue = createTokenIssue(now);
        authSessionRepository.createSession(
                userId,
                hashToken(tokenIssue.refreshToken()),
                hashToken(tokenIssue.accessToken()),
                tokenIssue.refreshExpiresAt(),
                tokenIssue.accessExpiresAt(),
                now);

        return new AuthSessionDto(
                userId,
                email,
                username,
                tokenIssue.accessToken(),
                tokenIssue.refreshToken(),
                Duration.between(now, tokenIssue.accessExpiresAt()).toSeconds(),
                TOKEN_TYPE);
    }

    private TokenIssue createTokenIssue(LocalDateTime now) {
        LocalDateTime accessExpiresAt = now.plusMinutes(accessTokenExpireMinutes);
        LocalDateTime refreshExpiresAt = now.plusDays(refreshTokenExpireDays);
        return new TokenIssue(generateToken(), generateToken(), accessExpiresAt, refreshExpiresAt);
    }

    private String generateToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("系统不支持SHA-256", ex);
        }
    }

    private String parseBearerToken(String authorizationHeader) {
        String value = normalize(authorizationHeader);
        String prefix = TOKEN_TYPE + " ";
        if (!value.startsWith(prefix)) {
            throw new UnauthorizedException("缺少有效的访问令牌");
        }
        String token = value.substring(prefix.length()).trim();
        if (token.isEmpty()) {
            throw new UnauthorizedException("缺少有效的访问令牌");
        }
        return token;
    }

    private record TokenIssue(String accessToken, String refreshToken, LocalDateTime accessExpiresAt,
            LocalDateTime refreshExpiresAt) {
    }
}
