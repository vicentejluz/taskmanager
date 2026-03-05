package com.vicente.taskmanager.controller;

import com.vicente.taskmanager.controller.docs.AuthControllerDoc;
import com.vicente.taskmanager.dto.request.*;
import com.vicente.taskmanager.dto.response.MessageResponseDTO;
import com.vicente.taskmanager.dto.response.TokenResponseDTO;
import com.vicente.taskmanager.dto.response.RegisterUserResponseDTO;
import com.vicente.taskmanager.domain.enums.TokenType;
import com.vicente.taskmanager.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
public class AuthController implements AuthControllerDoc {
    private final AuthService authService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public ResponseEntity<RegisterUserResponseDTO> register(RegisterUserRequestDTO registerUserRequest
    ) {
        logger.debug("POST /api/v1/auth/register called");
        RegisterUserResponseDTO registerUserResponseDTO =
                authService.register(registerUserRequest);

        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(registerUserResponseDTO.id())
                .toUri();

        return ResponseEntity.created(uri).body(registerUserResponseDTO);
    }

    @Override
    public ResponseEntity<TokenResponseDTO> login(LoginRequestDTO loginRequestDTO) {
        logger.debug("POST /api/v1/auth/login login called");
        TokenResponseDTO token = authService.login(loginRequestDTO);
        return ResponseEntity.ok(token);
    }

    @Override
    public ResponseEntity<MessageResponseDTO> resendEmailVerification(EmailRequestDTO emailRequestDTO) {
        logger.debug("POST /api/v1/auth/resend-email-verification resend verification called | email={}",
                emailRequestDTO.email());
        authService.sendTokenEmail(emailRequestDTO.email(), TokenType.EMAIL_VERIFICATION);
        return ResponseEntity.ok(new MessageResponseDTO("If the email exists, a message was sent"));
    }

    @Override
    public ResponseEntity<MessageResponseDTO> forgotPassword(EmailRequestDTO emailRequestDTO) {
        logger.debug("POST /api/v1/auth/forgot-password forgot password called | email={}",
                emailRequestDTO.email());
        authService.sendTokenEmail(emailRequestDTO.email(), TokenType.PASSWORD_RESET);
        return ResponseEntity.ok(new MessageResponseDTO("If the email exists, a message was sent"));
    }

    @Override
    public ResponseEntity<MessageResponseDTO> verifyEmail(String token) {
        logger.debug("GET /api/v1/auth/verify-email verify email called");
        authService.verifyEmail(token);
        return ResponseEntity.ok(new MessageResponseDTO(
                "Email has been successfully verified. You can now log in"));
    }

    @Override
    public ResponseEntity<MessageResponseDTO> validateToken(String token) {
        logger.debug("GET /api/v1/auth/password-reset reset password called");
        authService.validateToken(token);
        return ResponseEntity.ok(new MessageResponseDTO("Token is valid. You can now reset your password"));
    }

    @Override
    public ResponseEntity<MessageResponseDTO> passwordReset(
            String token, PasswordRequestDTO  passwordRequestDTO, HttpServletRequest request
    ) {
        logger.debug("PATCH /api/v1/auth/password-reset reset password called");
        String ipAddress = request.getHeader("X-Forwarded-For") != null
                ? request.getHeader("X-Forwarded-For").split(",")[0].trim()
                : request.getRemoteAddr();
        authService.passwordReset(token, passwordRequestDTO, ipAddress);
        return ResponseEntity.ok(new MessageResponseDTO(
                "Your password has been reset successfully. You can now log in with your new password."));
    }

    @Override
    public ResponseEntity<TokenResponseDTO> refreshToken(RefreshTokenRequestDTO refreshTokenRequestDTO) {
        logger.debug("POST /api/v1/auth/refresh-token refresh token called");
        TokenResponseDTO tokenResponseDTO = authService.refreshToken(refreshTokenRequestDTO);
        return ResponseEntity.ok(tokenResponseDTO);
    }
}
