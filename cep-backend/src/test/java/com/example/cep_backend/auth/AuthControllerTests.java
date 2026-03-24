package com.example.cep_backend.auth;

import com.example.cep_backend.common.api.ApiResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.cep_backend.auth.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTests {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private EmailService emailService;

  @Test
  void shouldRegisterAndLoginSuccessfully() throws Exception {
    doNothing().when(emailService).sendRegisterCode(anyString(), anyString());

    String email = "student1@example.com";
    String password = "abc12345";

    mockMvc.perform(post("/api/auth/send-register-code")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "email": "%s"
            }
            """.formatted(email)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    String code = jdbcTemplate.queryForObject(
        "SELECT TOP 1 code FROM email_verification_codes WHERE email = ? ORDER BY created_at DESC",
        String.class,
        email);

    mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "email": "%s",
              "code": "%s",
              "username": "测试用户",
              "password": "%s"
            }
            """.formatted(email, code, password)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.email").value(email));

    String loginResponse = mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "email": "%s",
              "password": "%s"
            }
            """.formatted(email, password)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("登录成功"))
        .andExpect(jsonPath("$.data.accessToken").isString())
        .andExpect(jsonPath("$.data.refreshToken").isString())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode loginJson = objectMapper.readTree(loginResponse);
    String accessToken = loginJson.path("data").path("accessToken").asText();
    String refreshToken = loginJson.path("data").path("refreshToken").asText();

    mockMvc.perform(get("/api/auth/me")
        .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.email").value(email));

    mockMvc.perform(post("/api/auth/refresh")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "refreshToken": "%s"
            }
            """.formatted(refreshToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.accessToken").isString())
        .andExpect(jsonPath("$.data.refreshToken").isString());
  }

  @Test
  void shouldRejectRegisterWhenCodeWrong() throws Exception {
    doNothing().when(emailService).sendRegisterCode(anyString(), anyString());

    String email = "student2@example.com";

    mockMvc.perform(post("/api/auth/send-register-code")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "email": "%s"
            }
            """.formatted(email)))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "email": "%s",
              "code": "000000",
              "username": "测试用户",
              "password": "abc12345"
            }
            """.formatted(email)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  void shouldResetPasswordSuccessfully() throws Exception {
    doNothing().when(emailService).sendRegisterCode(anyString(), anyString());
    doNothing().when(emailService).sendResetPasswordCode(anyString(), anyString());

    String email = "student3@example.com";
    String oldPassword = "abc12345";
    String newPassword = "xyz12345";

    mockMvc.perform(post("/api/auth/send-register-code")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "email": "%s"
            }
            """.formatted(email)))
        .andExpect(status().isOk());

    String registerCode = jdbcTemplate.queryForObject(
        "SELECT TOP 1 code FROM email_verification_codes WHERE email = ? AND purpose = 'REGISTER' ORDER BY created_at DESC",
        String.class,
        email);

    mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "email": "%s",
              "code": "%s",
              "username": "测试用户",
              "password": "%s"
            }
            """.formatted(email, registerCode, oldPassword)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    mockMvc.perform(post("/api/auth/send-reset-password-code")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "email": "%s"
            }
            """.formatted(email)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    String resetCode = jdbcTemplate.queryForObject(
        "SELECT TOP 1 code FROM email_verification_codes WHERE email = ? AND purpose = 'RESET_PASSWORD' ORDER BY created_at DESC",
        String.class,
        email);

    mockMvc.perform(post("/api/auth/reset-password")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "email": "%s",
              "code": "%s",
              "password": "%s"
            }
            """.formatted(email, resetCode, newPassword)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "email": "%s",
              "password": "%s"
            }
            """.formatted(email, oldPassword)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));

    mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "email": "%s",
              "password": "%s"
            }
            """.formatted(email, newPassword)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  void shouldShowUnregisteredMessageWhenEmailNotExists() throws Exception {
    mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "email": "not-exists@example.com",
              "password": "abc12345"
            }
            """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("该邮箱尚未注册"));
  }

  @Test
  void shouldVerifyCodesAndLogoutSuccessfully() throws Exception {
    doNothing().when(emailService).sendRegisterCode(anyString(), anyString());
    doNothing().when(emailService).sendResetPasswordCode(anyString(), anyString());

    String email = "student4@example.com";
    String password = "abc12345";

    mockMvc.perform(post("/api/auth/send-register-code")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "email": "%s"
            }
            """.formatted(email)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    String registerCode = jdbcTemplate.queryForObject(
        "SELECT TOP 1 code FROM email_verification_codes WHERE email = ? AND purpose = 'REGISTER' ORDER BY created_at DESC",
        String.class,
        email);

    mockMvc.perform(post("/api/auth/verify-register-code")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "email": "%s",
              "code": "%s"
            }
            """.formatted(email, registerCode)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("验证码校验通过"));

    mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "email": "%s",
              "code": "%s",
              "username": "测试用户",
              "password": "%s"
            }
            """.formatted(email, registerCode, password)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    mockMvc.perform(post("/api/auth/send-reset-password-code")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "email": "%s"
            }
            """.formatted(email)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    String resetCode = jdbcTemplate.queryForObject(
        "SELECT TOP 1 code FROM email_verification_codes WHERE email = ? AND purpose = 'RESET_PASSWORD' ORDER BY created_at DESC",
        String.class,
        email);

    mockMvc.perform(post("/api/auth/verify-reset-password-code")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "email": "%s",
              "code": "%s"
            }
            """.formatted(email, resetCode)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("验证码校验通过"));

    String loginResponse = mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "email": "%s",
              "password": "%s"
            }
            """.formatted(email, password)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode loginJson = objectMapper.readTree(loginResponse);
    String refreshToken = loginJson.path("data").path("refreshToken").asText();

    mockMvc.perform(post("/api/auth/logout")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "refreshToken": "%s"
            }
            """.formatted(refreshToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("退出成功"));

    mockMvc.perform(post("/api/auth/refresh")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "refreshToken": "%s"
            }
            """.formatted(refreshToken)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }
}
