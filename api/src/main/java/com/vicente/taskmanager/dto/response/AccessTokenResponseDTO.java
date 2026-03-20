package com.vicente.taskmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "AccessTokenResponse",
        description = "Response containing a newly generated JWT access token"
)
public record AccessTokenResponseDTO(

        @Schema(
                description = "JWT access token used to authenticate API requests",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        )
        String accessToken,

        @Schema(
                description = "Authentication scheme used in the Authorization header",
                example = "Bearer"
        )
        String tokenType
) {

    public AccessTokenResponseDTO(String accessToken) {
        this(accessToken, "Bearer");
    }
}
