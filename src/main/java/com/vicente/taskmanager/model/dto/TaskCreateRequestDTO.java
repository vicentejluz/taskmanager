package com.vicente.taskmanager.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Schema(description = "Task create request representation")
public record TaskCreateRequestDTO(
        @Schema(
                description = "Title of the task",
                example = "Study Spring Boot"
        )
        @NotBlank @Size(max = 50) String title,
        @Schema(
                description = "Detailed description of the task",
                example = "Finish REST API module"

        )
        @Size(max = 600) String description,
        @Schema(
                description = "Task due date",
                example = "2026-03-10"
        )
        @NotNull @FutureOrPresent LocalDate dueDate) {
}
