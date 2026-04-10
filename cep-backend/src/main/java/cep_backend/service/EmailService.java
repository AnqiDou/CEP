package cep_backend.service;

public interface EmailService {
    void sendRegisterCode(String targetEmail, String code);

    void sendResetPasswordCode(String targetEmail, String code);
}
