package com.vicente.taskmanager.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "RefreshTokenRequest", description = "Request used to obtain a new access token using a refresh token")
public record RefreshTokenRequestDTO(
        @Schema(
                description = "Valid refresh token issued during authentication",
                example = "d8f2c1f3-6a4e-4b3f-b8b2-9f1a2b3c4d5e"
        )
        @NotBlank(message = "is required")
        String refreshToken) {
}
