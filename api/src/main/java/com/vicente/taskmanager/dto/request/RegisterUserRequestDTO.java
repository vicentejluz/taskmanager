package com.vicente.taskmanager.dto.request;

import com.vicente.taskmanager.validation.constraints.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "RegisterUserRequest", description = "Request payload for registering a new user")
public record RegisterUserRequestDTO(

        @Schema(description = "User's full name (3 to 60 characters)",
                example = "Vicente Luz")
        @NotBlank(message = "is required")
        @Size(min = 3, max = 60)
        String name,

        @Schema(description = "Valid email address",
                example = "vicente@email.com")
        @NotBlank(message = "is required")
        @Email
        String email,

        @Schema(description = """
                User password.
                Must contain at least 8 characters, one uppercase letter,
                one lowercase letter, one number and one special character (@$!%*?&#.).
                Special characters with accent are not allowed.
                """,
                example = "Strong@123")
        @NotBlank(message = "is required")
        @ValidPassword
        String password

) {
}
