package com.vicente.taskmanager.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "FileStorageRenameRequestDTO", description = "Request payload to rename a file")
public record FileStorageRenameRequestDTO(
        @NotBlank(message = "is required")
        @Schema(description = "New name for the file (without extension)", example = "my-renamed-file")
        String newFileName
) {}
