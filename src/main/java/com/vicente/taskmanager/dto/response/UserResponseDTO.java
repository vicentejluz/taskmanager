package com.vicente.taskmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

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
        String email,

        @Schema(
                description = "Date and time when the user was created",
                example = "2026-02-15T10:15:30Z"
        )
        OffsetDateTime createdAt,

        @Schema(
                description = "Date and time when the user was last updated",
                example = "2026-02-15T12:40:00Z"
        )
        OffsetDateTime updatedAt
) {
}


