package com.example.cep_backend.auth.service;

import com.example.cep_backend.auth.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

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
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(targetEmail);
            message.setSubject(subject);
            message.setText("您好，您本次注册验证码为：" + code + "，10分钟内有效。若非本人操作请忽略。");
            mailSender.send(message);
        } catch (MailAuthenticationException ex) {
            log.error("SMTP authentication failed for from={}", from, ex);
            throw new BusinessException("邮箱认证失败，请检查 QQ 邮箱账号与授权码");
        } catch (MailSendException ex) {
            log.error("SMTP send failed for to={} from={}", targetEmail, from, ex);
            throw new BusinessException("邮件投递失败，请检查网络连接或收件邮箱地址");
        } catch (MailException ex) {
            log.error("SMTP mail exception for to={} from={}", targetEmail, from, ex);
            throw new BusinessException("邮件发送失败，请检查 SMTP 配置或稍后重试");
        }
    }

    @Override
    public void sendResetPasswordCode(String targetEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(targetEmail);
            message.setSubject(subject + "（找回密码）");
            message.setText("您好，您本次重置密码验证码为：" + code + "，10分钟内有效。若非本人操作请忽略。");
            mailSender.send(message);
        } catch (MailAuthenticationException ex) {
            log.error("SMTP authentication failed for from={}", from, ex);
            throw new BusinessException("邮箱认证失败，请检查 QQ 邮箱账号与授权码");
        } catch (MailSendException ex) {
            log.error("SMTP send failed for to={} from={}", targetEmail, from, ex);
            throw new BusinessException("邮件投递失败，请检查网络连接或收件邮箱地址");
        } catch (MailException ex) {
            log.error("SMTP mail exception for to={} from={}", targetEmail, from, ex);
            throw new BusinessException("邮件发送失败，请检查 SMTP 配置或稍后重试");
        }
    }
}
