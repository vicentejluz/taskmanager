package com.vicente.taskmanager.service;

public interface EmailService {
    void sendVerificationEmail(String email, String verificationToken);
    void sendForgotPasswordEmail(String email, String resetToken);
    void sendPasswordResetSuccessEmail(String email, String ipAddress);
}
