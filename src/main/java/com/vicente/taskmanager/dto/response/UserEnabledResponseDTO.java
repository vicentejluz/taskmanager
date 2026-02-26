package com.vicente.taskmanager.dto.response;

import com.vicente.taskmanager.model.enums.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "UserEnabledResponse",
        description = "Represents the basic activation status information of a user account"
)
public record UserEnabledResponseDTO(

        @Schema(
                description = "Unique identifier of the user",
                example = "1"
        )
        Long id,

        @Schema(
                description = "Current account status of the user",
                example = "ACTIVE"
        )
        AccountStatus accountStatus,

        @Schema(
                description = "Indicates whether the user account is enabled",
                example = "true"
        )
        Boolean enabled
) {}
