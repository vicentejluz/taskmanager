package com.vicente.taskmanager.service;

public interface EmailService {
    void sendVerificationEmail(String email, String verificationToken);
}
