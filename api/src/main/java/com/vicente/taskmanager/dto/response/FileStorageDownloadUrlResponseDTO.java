package com.vicente.taskmanager.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;


@Schema(name = "FileStorageDownloadUrlResponse", description = "Response containing a signed URL for file download")
public record FileStorageDownloadUrlResponseDTO(

        @Schema(
                description = "File ID",
                example = "123"
        )
        Long id,

        @Schema(
                description = "Signed URL used to download the file",
                example = "https://storage.example.com/file?sig=abc123"
        )
        String url,

        @Schema(
                description = "Signed URL expiration date and time",
                example = "2026-04-03T15:30:00Z"
        )
        OffsetDateTime expireAt
) {
}
