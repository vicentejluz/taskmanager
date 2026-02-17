package com.vicente.taskmanager.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Schema(name = "UserUpdateRequest", description = "Request object used for partial user update")
public record UserUpdateRequestDTO(

        @Schema(
                description = "Full name of the user",
                example = "Jane Smith"
        )
        @Size(min = 3, max = 60)
        String name,

        @Schema(
                description = "Valid email address of the user",
                example = "jane.smith@email.com"
        )
        @Email
        String email
) {
}

