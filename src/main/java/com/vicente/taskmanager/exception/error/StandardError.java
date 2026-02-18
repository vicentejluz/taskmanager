package com.vicente.taskmanager.exception.error;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Standard error response returned when an API request fails.")
public record StandardError(
        @Schema(
                description = "Timestamp when the error occurred in ISO-8601 format.",
                example = "2026-02-15T14:32:10Z"
        )
        Instant timestamp,

        @Schema(description = "HTTP status code of the error.")
        Integer status,

        @Schema(description = "HTTP status error description.")
        String error,

        @Schema(description = "Detailed message describing the error.")
        String message,

        @Schema(description = "API endpoint path where the error occurred.")
        String path) {
}
