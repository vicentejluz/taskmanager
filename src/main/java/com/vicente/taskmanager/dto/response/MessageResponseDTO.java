package com.vicente.taskmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "MessageResponse",
        description = "Generic response object used to return informational messages."
)
public record MessageResponseDTO(

        @Schema(
                description = "Human-readable message describing the result of the operation.",
                example = "If the email exists, a message was sent"
        )
        String message

) {}
