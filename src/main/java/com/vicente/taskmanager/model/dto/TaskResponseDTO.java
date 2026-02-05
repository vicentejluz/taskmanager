package com.vicente.taskmanager.model.dto;

import com.vicente.taskmanager.model.domain.TaskStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record TaskResponseDTO(
        Long id,
        String title,
        String description,
        LocalDate dueDate,
        TaskStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt){
}
