package com.vicente.taskmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Response returned after successfully registering a user")
public record RegisterUserResponseDTO(

        @Schema(description = "Unique identifier of the user", example = "1")
        Long id,

        @Schema(description = "Full name of the user", example = "Vicente Luz")
        String name,

        @Schema(description = "User's email address", example = "vicente@email.com")
        String email,

        @Schema(description = "Date and time when the user was created",
                example = "2026-02-12T01:45:30Z")
        OffsetDateTime createdAt,

        @Schema(description = "Date and time when the user was last updated",
                example = "2026-02-12T02:10:15Z")
        OffsetDateTime updatedAt

) {
}
