package com.vicente.taskmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TokenResponse", description = "Response returned after successful token generation")
public record TokenResponseDTO(
        @Schema(
                description = "JWT access token used for authenticated requests",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        )
        String accessToken,

        @Schema(
                description = "Refresh token used to obtain a new access token",
                example = "d8f2c1f3-6a4e-4b3f-b8b2-9f1a2b3c4d5e"
        )
        String refreshToken
) {}
