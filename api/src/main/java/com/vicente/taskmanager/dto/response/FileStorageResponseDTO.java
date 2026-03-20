package com.vicente.taskmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(
        name = "FileStorageResponse",
        description = ""
)
public record FileStorageResponseDTO(
        Long taskId,
        UUID id,
        String fileName,
        String contentType,
        Long size,
        OffsetDateTime createdAt
) {
}
