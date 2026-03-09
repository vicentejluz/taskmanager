package com.vicente.taskmanager.controller.docs;

import com.vicente.taskmanager.domain.entity.User;
import com.vicente.taskmanager.dto.request.*;
import com.vicente.taskmanager.dto.response.AccessTokenResponseDTO;
import com.vicente.taskmanager.dto.response.MessageResponseDTO;
import com.vicente.taskmanager.dto.response.RegisterUserResponseDTO;
import com.vicente.taskmanager.exception.error.LockedError;
import com.vicente.taskmanager.exception.error.StandardError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

@Tag(name = "Authentication", description = "Authentication endpoints")
public interface AuthControllerDoc {
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
    ResponseEntity<RegisterUserResponseDTO> register(RegisterUserRequestDTO registerUserRequest);

    @Operation(
            summary = "Authenticate user",
            description =
                """
                Authenticates a user using email and password.
                Returns a JWT access token in the response body and sets a refresh token
                as an HTTP-only cookie.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Authentication successful",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AccessTokenResponseDTO.class)
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
    ResponseEntity<AccessTokenResponseDTO> login(LoginRequestDTO loginRequestDTO, String refreshToken);

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
                            schema = @Schema(implementation = MessageResponseDTO.class)
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
    ResponseEntity<MessageResponseDTO> resendEmailVerification(EmailRequestDTO emailRequestDTO);

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
                            schema = @Schema(implementation = MessageResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request (e.g., invalid email format)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "User is not eligible for password reset (e.g., not active or deleted)",
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
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    ResponseEntity<MessageResponseDTO> forgotPassword(EmailRequestDTO emailRequestDTO);

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
                            schema = @Schema(implementation = MessageResponseDTO.class)
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
    ResponseEntity<MessageResponseDTO> verifyEmail(String token);

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
                            schema = @Schema(implementation = MessageResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid, expired, used, or revoked token",
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
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    ResponseEntity<MessageResponseDTO> validateToken(String token);

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
                            schema = @Schema(implementation = MessageResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid token, expired token, wrong token type, or invalid account status",
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
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    ResponseEntity<MessageResponseDTO> passwordReset(
            String token,
            PasswordRequestDTO passwordRequestDTO,
            HttpServletRequest request);

    @Operation(
            summary = "Refresh access token",
            description = """
        Generates a new JWT access token using a valid refresh token.

        The refresh token is expected to be sent automatically as an HTTP-only cookie.

        The following validations are performed:
        - Refresh token must be present
        - Refresh token must not be expired
        - Refresh token must not be revoked
        - The associated user must exist
        """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Access token successfully generated",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AccessTokenResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request (e.g., malformed request or missing data)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized (refresh token expired, revoked, or user not found)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    ResponseEntity<AccessTokenResponseDTO> refreshToken(@Parameter(hidden = true) String refreshToken);

    @Operation(
            summary = "Logout user",
            description = """
        Logs out the authenticated user by revoking the refresh token.

        The refresh token is expected to be sent automatically as an HTTP-only cookie.

        If the refresh token is valid, it will be revoked and the cookie will be removed
        from the client browser.
        """
    )
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "204",
                    description = "Logout successful. Refresh token revoked and cookie removed."
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request (e.g., malformed token)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),

            @ApiResponse(
                    responseCode = "401",
                    description = "Refresh token invalid or expired",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),

            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    ResponseEntity<Void> logout(@Parameter(hidden = true) String token, User user, HttpServletRequest request);
}
