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
                description = "Filters users by enabled status (true = enabled, false = disabled)",
                example = "true"
        )
        Boolean enabled,

        @Schema(
                description = "Filters users by account lock status (true = account not locked, false = locked)",
                example = "true"
        )
        Boolean accountNonLocked

) {}
