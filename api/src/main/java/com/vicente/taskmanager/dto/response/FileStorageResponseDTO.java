package com.vicente.taskmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(
        name = "FileStorageResponse",
        description = "Represents a stored file associated with a task."
)
public record FileStorageResponseDTO(

        @Schema(
                description = "ID of the task associated with the file",
                example = "123"
        )
        Long taskId,

        @Schema(
                description = "Unique identifier of the file",
                example = "550e8400-e29b-41d4-a716-446655440000",
                format = "uuid"
        )
        UUID id,

        @Schema(
                description = "Original file name",
                example = "document.pdf"
        )
        String fileName,

        @Schema(
                description = "File content type (MIME type)",
                example = "application/pdf"
        )
        String contentType,

        @Schema(
                description = "File size in bytes",
                example = "204800"
        )
        Long size,

        @Schema(
                description = "File creation date and time",
                example = "2026-03-20T14:30:00Z",
                format = "date-time"
        )
        OffsetDateTime createdAt
) {
}
