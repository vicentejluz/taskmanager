package com.vicente.taskmanager.exception.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record LockedError(
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
        String path,

        @Schema(
                description = """
                        Date and time when the account lock will expire.
                        This field is only present when the error is related to an account lock.
                        The value is returned in ISO-8601 UTC format.
                        """,
                example = "2026-02-19T23:16:19Z"
        )
        Instant lockedUntil
) {
}
