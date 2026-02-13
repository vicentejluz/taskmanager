package com.vicente.taskmanager.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request payload for user authentication")
public record LoginRequestDTO(

        @Schema(description = "User's registered email address",
                example = "vicente@email.com")
        @NotBlank(message = "is required")
        String email,

        @Schema(description = "User password",
                example = "Strong@123")
        @NotBlank(message = "is required")
        String password

) {
}
