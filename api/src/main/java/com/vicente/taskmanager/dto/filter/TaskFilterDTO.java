package com.vicente.taskmanager.dto.filter;

import com.vicente.taskmanager.domain.enums.TaskStatus;

import java.time.LocalDate;

public record TaskFilterDTO(Long userId, TaskStatus status, LocalDate dueDate) {
}
