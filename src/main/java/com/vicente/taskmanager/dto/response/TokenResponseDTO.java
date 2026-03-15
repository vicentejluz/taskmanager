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
                example = "wJ8pLr2J7rQK5vXo7aP1mC0vF5y9uZx6kD8tN4sE3A"
        )
        String refreshToken,

        @Schema(
                description = "Fingerprint identifier associated with the refresh token session. " +
                        "It must be stored by the client (usually in a secure cookie) and sent together " +
                        "with the refresh token when requesting a new access token. Used to detect token misuse.",
                example = "b7f5c9e8a2d44c6a9f8b1e2d3c4f5a6b"
        )
        String fingerprint
) {}
