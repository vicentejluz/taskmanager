package com.vicente.taskmanager.controller;

import com.vicente.taskmanager.dto.request.EmailRequestDTO;
import com.vicente.taskmanager.dto.request.LoginRequestDTO;
import com.vicente.taskmanager.dto.request.PasswordRequestDTO;
import com.vicente.taskmanager.dto.request.RegisterUserRequestDTO;
import com.vicente.taskmanager.dto.response.LoginResponseDTO;
import com.vicente.taskmanager.dto.response.RegisterUserResponseDTO;
import com.vicente.taskmanager.exception.error.LockedError;
import com.vicente.taskmanager.exception.error.StandardError;
import com.vicente.taskmanager.model.enums.TokenType;
import com.vicente.taskmanager.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Tag(name = "Authentication", description = "Authentication endpoints")
@RestController
@RequestMapping(value = "/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with name, email and password. Returns the created user information."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "User successfully registered",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RegisterUserResponseDTO.class)
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),

            @ApiResponse(
                    responseCode = "409",
                    description = "Email already registered",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponseDTO> register(
            @Valid @RequestBody RegisterUserRequestDTO registerUserRequest
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


    @Operation(
            summary = "Authenticate user",
            description = "Authenticates a user using email and password and returns a JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Authentication successful",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponseDTO.class)
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),

            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid email or password",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "423",
                    description = "Account Locked",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LockedError.class)
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        logger.debug("POST /api/v1/login login called");
        LoginResponseDTO token = authService.login(loginRequestDTO);
        return ResponseEntity.ok(token);
    }

    @Operation(
            summary = "Resend email verification link",
            description = "Sends a new email verification link if the account is pending verification."
    )
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "Verification email sent (if account exists and is eligible)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"If the email exists, a message was sent\"}")
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid email format",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),

            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    @PostMapping("/resend-email-verification")
    public ResponseEntity<Map<String, String>> resendEmailVerification(
            @Valid @RequestBody EmailRequestDTO emailRequestDTO) {
        logger.debug("POST /api/v1/resend-email-verification resend verification called | email={}",
                emailRequestDTO.email());
        authService.sendTokenEmail(emailRequestDTO.email(), TokenType.EMAIL_VERIFICATION);
        return ResponseEntity.ok(Map.of("message", "If the email exists, a message was sent"));
    }

    @Operation(
            summary = "Request password reset",
            description = """
                Generates (or reuses) a valid password reset token and sends an email
                to the user if the account exists and is eligible for password reset.

                Security note:
                The response message is always generic to prevent email enumeration attacks.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "If the email exists, a reset message was sent",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = """
                                {
                                  "message": "If the email exists, a message was sent"
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request (e.g., invalid email format)",
                    content = @Content(
                            mediaType = "application/json"
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "User is not eligible for password reset (e.g., not active or deleted)",
                    content = @Content(
                            mediaType = "application/json"
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(
                            mediaType = "application/json"
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json"
                    )
            )
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody EmailRequestDTO emailRequestDTO) {
        logger.debug("POST /api/v1/forgot-password forgot password called | email={}",
                emailRequestDTO.email());
        authService.sendTokenEmail(emailRequestDTO.email(), TokenType.PASSWORD_RESET);
        return ResponseEntity.ok(Map.of("message", "If the email exists, a message was sent"));
    }

    @Operation(
            summary = "Verify email address",
            description = "Validates the email verification token and activates the user account."
    )
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "Email successfully verified",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"message\": \"Email has been successfully verified. You can now log in\"}")
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),

            @ApiResponse(
                    responseCode = "404",
                    description = "Token not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam("token") String token) {
        logger.debug("GET /api/v1/verify-email verify email called");
        authService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message",
                "Email has been successfully verified. You can now log in"));
    }

    @Operation(
            summary = "Validate password reset token",
            description = """
                Validates a password reset token before allowing the user to define a new password.

                This endpoint checks whether the token:
                - Exists
                - Is not expired
                - Has not been used
                - Has not been revoked

                If valid, the client can proceed with the password reset request.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token is valid and can be used to reset the password",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = """
                                {
                                  "message": "Token is valid. You can now reset your password"
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid, expired, used, or revoked token",
                    content = @Content(
                            mediaType = "application/json"
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Token not found",
                    content = @Content(
                            mediaType = "application/json"
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json"
                    )
            )
    })
    @GetMapping("/password-reset")
    public ResponseEntity<Map<String, String>> validateToken(@RequestParam("token") String token) {
        logger.debug("GET /api/v1/reset-password reset password called");
        authService.validateToken(token);
        return ResponseEntity.ok(Map.of("message", "Token is valid. You can now reset your password"));
    }

    @Operation(
            summary = "Reset user password",
            description = """
                Resets the user's password using a valid password reset token.

                This endpoint performs the following validations:
                - Token must exist
                - Token must be of type PASSWORD_RESET
                - Token must not be expired, used, or revoked
                - User account must be ACTIVE
                - User must not be deleted

                If all validations pass, the password is updated and the token is consumed.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Password reset successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = """
                                {
                                  "message": "Your password has been reset successfully.
                                   You can now log in with your new password."
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid token, expired token, wrong token type, or invalid account status",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Token not found",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PatchMapping("/password-reset")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestParam("token") String token,
            @Valid @RequestBody PasswordRequestDTO  passwordRequestDTO,
            HttpServletRequest request
    ) {
        logger.debug("PATCH /api/v1/reset-password reset password called");
        String ipAddress = request.getHeader("X-Forwarded-For") != null
                ? request.getHeader("X-Forwarded-For")
                : request.getRemoteAddr();
        authService.passwordReset(token, passwordRequestDTO, ipAddress);
        return ResponseEntity.ok(Map.of("message",
                "Your password has been reset successfully. You can now log in with your new password."));
    }
}
