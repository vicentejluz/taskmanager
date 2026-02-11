package com.vicente.taskmanager.model.dto;

import com.vicente.taskmanager.model.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Schema(description = "Task response representation")
public record TaskResponseDTO(
        @Schema(
                description = "Unique identifier of the task",
                example = "1"
        )
        Long id,

        @Schema(
                description = "Title of the task",
                example = "Study Spring Boot"
        )
        String title,

        @Schema(
                description = "Detailed description of the task",
                example = "Finish REST API module and review JPA mappings"
        )
        String description,

        @Schema(
                description = "Task due date",
                example = "2026-03-10"
        )
        LocalDate dueDate,

        @Schema(
                description = "Current status of the task",
                example = "IN_PROGRESS"
        )
        TaskStatus status,

        @Schema(
                description = "Date and time when the task was created",
                example = "2026-02-05T00:15:30Z"
        )
        OffsetDateTime createdAt,

        @Schema(
                description = "Date and time when the task was last updated",
                example = "2026-02-06T14:42:10Z"
        )
        OffsetDateTime updatedAt){
}
