package com.vicente.taskmanager.dto.request;

import com.vicente.taskmanager.validation.constraints.ValidPassword;
import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PasswordRequest", description = "Request body containing the new password.")
public record PasswordRequestDTO(

        @Schema(
                description = "New password that meets the security requirements.",
                example = "Str0ngP@ssword!"
        )
        @NotBlank(message = "Password is required")
        @ValidPassword
        String password
) {
}
