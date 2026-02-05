package com.vicente.taskmanager.model.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record TaskRequestDTO(
        @NotBlank @Size(max = 50) String title,
        @Size(max = 600) String description,
        @NotNull @FutureOrPresent LocalDate dueDate) {
}
