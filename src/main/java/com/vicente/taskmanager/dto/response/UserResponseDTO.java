package com.vicente.taskmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserResponse", description = "Represents the data returned for a user")
public record UserResponseDTO(

        @Schema(
                description = "Unique identifier of the user",
                example = "1"
        )
        Long id,

        @Schema(
                description = "Full name of the user",
                example = "John Doe"
        )
        String name,

        @Schema(
                description = "Email address of the user",
                example = "john.doe@email.com"
        )
        String email
) {
}


