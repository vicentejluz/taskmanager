package com.vicente.taskmanager.controller;

import com.vicente.taskmanager.controller.docs.AuthControllerDoc;
import com.vicente.taskmanager.domain.entity.User;
import com.vicente.taskmanager.dto.request.*;
import com.vicente.taskmanager.dto.response.AccessTokenResponseDTO;
import com.vicente.taskmanager.dto.response.MessageResponseDTO;
import com.vicente.taskmanager.dto.response.TokenResponseDTO;
import com.vicente.taskmanager.dto.response.RegisterUserResponseDTO;
import com.vicente.taskmanager.domain.enums.TokenType;
import com.vicente.taskmanager.security.TokenExtractor;
import com.vicente.taskmanager.service.AuthService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;

@RestController
@RequestMapping(value = "/api/v1/auth")
public class AuthController implements AuthControllerDoc {
    private final AuthService authService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponseDTO> register(
            @Valid @RequestBody RegisterUserRequestDTO registerUserRequest) {
        logger.debug("POST /api/v1/auth/register called");
        RegisterUserResponseDTO registerUserResponseDTO =
                authService.register(registerUserRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(registerUserResponseDTO);
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO loginRequestDTO,
            @CookieValue(value = "refreshToken", required = false) String refreshToken
    ) {
        logger.debug("POST /api/v1/auth/login login called | email={}", loginRequestDTO.email());
        TokenResponseDTO tokenResponseDTO = authService.login(loginRequestDTO, refreshToken);
        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokenResponseDTO.refreshToken())
                .path("/api/v1")
                .httpOnly(true)
                .sameSite("Strict")
                .maxAge(Duration.ofDays(12))
                .build();
        AccessTokenResponseDTO accessToken = new AccessTokenResponseDTO(tokenResponseDTO.accessToken());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(accessToken);
    }

    @Override
    @PostMapping("/resend-email-verification")
    public ResponseEntity<MessageResponseDTO> resendEmailVerification(@Valid @RequestBody EmailRequestDTO emailRequestDTO) {
        logger.debug("POST /api/v1/auth/resend-email-verification resend verification called | email={}",
                emailRequestDTO.email());
        authService.sendTokenEmail(emailRequestDTO.email(), TokenType.EMAIL_VERIFICATION);
        return ResponseEntity.ok(new MessageResponseDTO("If the email exists, a message was sent"));
    }

    @Override
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponseDTO> forgotPassword(@Valid @RequestBody EmailRequestDTO emailRequestDTO) {
        logger.debug("POST /api/v1/auth/forgot-password forgot password called | email={}",
                emailRequestDTO.email());
        authService.sendTokenEmail(emailRequestDTO.email(), TokenType.PASSWORD_RESET);
        return ResponseEntity.ok(new MessageResponseDTO("If the email exists, a message was sent"));
    }

    @Override
    @GetMapping("/verify-email")
    public ResponseEntity<MessageResponseDTO> verifyEmail(@RequestParam("token") String token, HttpServletRequest request) {
        logger.debug("GET /api/v1/auth/verify-email verify email called");
        String ipAddress = request.getHeader("X-Forwarded-For") != null
                ? request.getHeader("X-Forwarded-For").split(",")[0].trim()
                : request.getRemoteAddr();
        authService.verifyEmail(token, ipAddress);
        return ResponseEntity.ok(new MessageResponseDTO(
                "Email has been successfully verified. You can now log in"));
    }

    @Override
    @GetMapping("/password-reset")
    public ResponseEntity<MessageResponseDTO> validateToken(@RequestParam("token") String token) {
        logger.debug("GET /api/v1/auth/password-reset reset password called");
        authService.validateToken(token);
        return ResponseEntity.ok(new MessageResponseDTO("Token is valid. You can now reset your password"));
    }

    @Override
    @PatchMapping("/password-reset")
    public ResponseEntity<MessageResponseDTO> passwordReset(
            @RequestParam("token") String token,
            @Valid @RequestBody PasswordRequestDTO  passwordRequestDTO,
            HttpServletRequest request
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
    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponseDTO> refreshToken(
            @Parameter(hidden = true)
            @CookieValue(value = "refreshToken", required = false)
            String refreshToken
    ) {
        logger.debug("POST /api/v1/auth/refresh refresh token called");
        TokenResponseDTO tokenResponseDTO = authService.refreshToken(refreshToken);
        AccessTokenResponseDTO accessToken = new AccessTokenResponseDTO(tokenResponseDTO.accessToken());
        return ResponseEntity.ok(accessToken);
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            @AuthenticationPrincipal User user,
            HttpServletRequest request
    ) {
        logger.debug("POST /api/v1/auth/logout logout called | userId={}", user.getId());
        String accessToken = TokenExtractor.extractAccessToken(request);
        authService.logout(refreshToken, accessToken, user.getId());
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .path("/api/v1")
                .httpOnly(true)
                .sameSite("Strict")
                .maxAge(0)
                .build();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }
}
