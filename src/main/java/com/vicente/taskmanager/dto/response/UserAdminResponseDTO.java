package com.vicente.taskmanager.dto.response;

import com.vicente.taskmanager.model.enums.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(
        name = "UserAdminResponse",
        description = "Represents the data returned for a user with administrative details"
)
public record UserAdminResponseDTO(

        @Schema(
                description = "Unique identifier of the user",
                example = "1"
        )
        Long id,

        @Schema(
                description = "Full name of the user",
                example = "John Doe"
        )
        String name,

        @Schema(
                description = "Email address of the user",
                example = "john.doe@email.com"
        )
        String email,

        @Schema(
                description = "Current account status of the user",
                example = "ACTIVE"
        )
        AccountStatus accountStatus,

        @Schema(
                description = "Indicates whether the user account is enabled",
                example = "true"
        )
        Boolean enabled,

        @Schema(
                description = "Indicates whether the user account is not locked",
                example = "true"
        )
        Boolean accountNonLocked,

        @Schema(
                description = "Date and time when the user was created",
                example = "2026-02-18T03:15:30Z"
        )
        OffsetDateTime createdAt,

        @Schema(
                description = "Date and time when the user was last updated",
                example = "2026-02-18T04:10:45Z"
        )
        OffsetDateTime updatedAt

) {}
