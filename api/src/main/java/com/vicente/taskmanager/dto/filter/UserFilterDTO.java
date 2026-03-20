package com.vicente.taskmanager.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Available filters for querying users")
public record UserFilterDTO(

        @Schema(
                description = "Filters users by name (partial and case-insensitive match)",
                example = "vicente"
        )
        String name,

        @Schema(
                description = "Filters users by account status. Possible values: PENDING_VERIFICATION, DISABLED_BY_ADMIN, ACTIVE",
                example = "ACTIVE"
        )
        String accountStatus

) {}
