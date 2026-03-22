package com.vicente.taskmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

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
                example = "123"
        )
        Long fileId,

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
