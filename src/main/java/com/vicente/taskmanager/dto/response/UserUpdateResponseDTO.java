package com.vicente.taskmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(
        name = "UserUpdateResponse",
        description = "Represents the data returned after a successful user update operation"
)
public record UserUpdateResponseDTO(

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
                example = "2026-02-18T03:15:30Z"
        )
        OffsetDateTime createdAt,

        @Schema(
                description = "Date and time when the user was last updated",
                example = "2026-02-18T04:10:45Z"
        )
        OffsetDateTime updatedAt

) {}


