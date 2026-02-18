package com.vicente.taskmanager.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(
        name = "UpdateUserEnabledRequest",
        description = "Request payload to enable or disable a user"
)
public record UpdateUserEnabledRequest(

        @Schema(
                description = "Indicates whether the user should be enabled or disabled",
                example = "true"
        )
        @NotNull(message = " is required.")
        Boolean enabled

) {}
