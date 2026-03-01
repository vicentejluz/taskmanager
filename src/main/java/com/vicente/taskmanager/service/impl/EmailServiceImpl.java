package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;
    private final String from;
    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    public EmailServiceImpl(JavaMailSender mailSender, @Value("${spring.mail.email}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Async
    @Override
    public void sendVerificationEmail(String email, String verificationToken) {
        String subject = "Email Verification";
        String path = "/api/v1/auth/verify-email";
        String message = "Click the button below to verify your email address:";
        sendActionEmail(email, verificationToken, subject, path, message);
    }

    @Async
    @Override
    public void sendForgotPasswordEmail(String email, String resetToken) {
        String subject = "Password Reset Request";
        String path = "/api/v1/auth/password-reset";
        String message = "Click the button below to reset your password:";
        sendActionEmail(email, resetToken, subject, path, message);
    }

    @Async
    @Override
    public void sendPasswordResetSuccessEmail(String email, String ipAddress) {
        String subject = "Your password has been changed successfully";
        String message =
               """
               This is a security notification to inform you that your account password was changed.

               If this action was performed by you, no further steps are required.

               If you did not authorize this change, we strongly recommend that you reset your password immediately and review your account activity.
               """;
        sendSecurityNotificationEmail(email, subject, message, ipAddress);
    }

    private void sendActionEmail(String email, String token, String subject, String path, String message) {
        try {
            String actionUrl = UriComponentsBuilder
                    .fromUriString("http://localhost:8080")
                    .path(path)
                    .queryParam("token", token)
                    .toUriString();

            String content = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border-radius: 8px; background-color: #f9f9f9; text-align: center;">
                        <h2 style="color: #333;">%s</h2>
                        <p style="font-size: 16px; color: #555;">%s</p>
                        <a href="%s" style="display: inline-block; margin: 20px 0; padding: 10px 20px; font-size: 16px; color: #fff; background-color: #007bff; text-decoration: none; border-radius: 5px;">Proceed</a>
                        <p style="font-size: 14px; color: #777;">Or copy and paste this link into your browser:</p>
                        <p style="font-size: 14px; color: #007bff;">%s</p>
                        <p style="font-size: 12px; color: #aaa;">This is an automated message. Please do not reply.</p>
                    </div>
                """.formatted(subject, message, actionUrl, actionUrl);

            sendHtmlEmail(email, subject, content);

        } catch (Exception e) {
            logger.error("Failed to send verification email to {}", email, e);
        }
    }

    private void sendSecurityNotificationEmail(String email, String subject, String message, String ipAddress) {
        try {
            String dateTime = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String content = buildSecurityNotificationEmail(subject, message, dateTime, ipAddress);

            sendHtmlEmail(email, subject, content);
        }catch (Exception e) {
            logger.error("Failed to send email to {}", email, e);
        }
    }

    private void sendHtmlEmail(String email, String subject, String content) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

        helper.setTo(email);
        helper.setSubject(subject);
        helper.setFrom(from);
        helper.setText(content, true);
        mailSender.send(mimeMessage);
    }

    private String buildSecurityNotificationEmail(
            String subject,
            String message,
            String dateTime,
            String ipAddress) {

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>%s</title>
        </head>
        <body style="margin:0;padding:0;background-color:#f4f6f8;font-family:Arial,sans-serif;">
            <table width="100%%" cellpadding="0" cellspacing="0">
                <tr>
                    <td align="center" style="padding:40px 0;">
                        <table width="600" cellpadding="0" cellspacing="0"
                               style="background:#ffffff;border-radius:8px;padding:30px;">
        
                            <tr>
                                <td style="font-size:20px;font-weight:bold;color:#1f2937;">
                                    %s
                                </td>
                            </tr>

                            <tr>
                                <td style="padding-top:20px;font-size:15px;color:#4b5563;line-height:1.6;">
                                    %s
                                </td>
                            </tr>

                            <tr>
                                <td style="padding-top:25px;font-size:13px;color:#6b7280;">
                                    <strong>Date & Time:</strong> %s<br>
                                    <strong>IP Address:</strong> %s
                                </td>
                            </tr>

                            <tr>
                                <td style="padding-top:30px;font-size:12px;color:#9ca3af;">
                                    This is an automated security notification. Please do not reply to this email.
                                </td>
                            </tr>

                        </table>
                    </td>
                </tr>
            </table>
        </body>
        </html>
        """.formatted(
                subject,
                subject,
                message.replace("\n", "<br>"),
                dateTime,
                ipAddress
        );
    }
}
