package com.example.cep_backend.auth.service;

import com.example.cep_backend.auth.BusinessException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class SmtpEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);
    private final JavaMailSender mailSender;
    private final String subject;
    private final String from;

    public SmtpEmailService(JavaMailSender mailSender,
            @Value("${app.auth.mail.subject}") String subject,
            @Value("${app.auth.mail.from}") String from) {
        this.mailSender = mailSender;
        this.subject = subject;
        this.from = from;
    }

    @Override
    public void sendRegisterCode(String targetEmail, String code) {
        sendTextMail(targetEmail, subject, "您好，您本次注册验证码为：" + code + "，10分钟内有效。若非本人操作请忽略。");
    }

    @Override
    public void sendResetPasswordCode(String targetEmail, String code) {
        sendTextMail(targetEmail, subject + "（找回密码）", "您好，您本次重置密码验证码为：" + code + "，10分钟内有效。若非本人操作请忽略。");
    }

    private void sendTextMail(String targetEmail, String mailSubject, String mailText) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(targetEmail);
            helper.setSubject(mailSubject);
            helper.setText(mailText, false);
            mailSender.send(message);
        } catch (MailAuthenticationException ex) {
            log.error("SMTP authentication failed for from={}", from, ex);
            throw new BusinessException("邮箱认证失败，请检查 QQ 邮箱账号与授权码");
        } catch (MessagingException ex) {
            log.error("SMTP message construct failed for to={} from={}", targetEmail, from, ex);
            throw new BusinessException("邮件内容构建失败，请检查邮件主题与内容编码配置");
        } catch (MailSendException ex) {
            log.error("SMTP send failed for to={} from={}", targetEmail, from, ex);
            throw new BusinessException("邮件投递失败，请检查网络连接或收件邮箱地址");
        } catch (MailException ex) {
            log.error("SMTP mail exception for to={} from={}", targetEmail, from, ex);
            throw new BusinessException("邮件发送失败，请检查 SMTP 配置或稍后重试");
        }
    }
}
