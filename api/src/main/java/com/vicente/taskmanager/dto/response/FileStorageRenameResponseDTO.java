package com.vicente.taskmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FileStorageRenameResponse", description = "Response after renaming a file in storage")
public record FileStorageRenameResponseDTO(
        @Schema(description = "ID of the renamed file", example = "123")
        Long id,

        @Schema(description = "New name of the file", example = "updated-document.txt")
        String newFileName
) {}
