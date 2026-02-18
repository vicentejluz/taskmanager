package com.vicente.taskmanager.dto.request;

import com.vicente.taskmanager.validation.constraints.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;


@Schema(
        name = "PasswordUpdateRequest",
        description = "Request object used to update the authenticated user's password"
)
public record PasswordUpdateRequestDTO(

        @Schema(
                description = "Current password of the authenticated user",
                example = "OldPassword123!"
        )
        @NotBlank(message = " is required")
        String oldPassword,

        @Schema(
                description = "New password that meets the security requirements",
                example = "NewStrongPassword@123"
        )
        @NotBlank(message = " is required")
        @ValidPassword
        String newPassword
) {}
