package com.vicente.taskmanager.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EmailRequest", description = "Request body containing the user's email address.")
public record EmailRequestDTO(

        @Schema(
                description = "User's email address.",
                example = "user@example.com"
        )
        @NotBlank(message = " is required")
        @Email(message = "Invalid email format")
        String email
) {
}
