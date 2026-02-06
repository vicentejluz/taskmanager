package com.vicente.taskmanager.mapper;

import com.vicente.taskmanager.model.domain.Task;
import com.vicente.taskmanager.model.dto.TaskCreateRequestDTO;
import com.vicente.taskmanager.model.dto.TaskResponseDTO;
import com.vicente.taskmanager.model.dto.TaskUpdateRequestDTO;

public final class TaskMapper {

    public static Task toEntity(TaskCreateRequestDTO request) {
        return new Task(request.title(), request.description(), request.dueDate());
    }

    public static TaskResponseDTO toDTO(Task task) {
        return  new TaskResponseDTO(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDueDate(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    public static void merge(Task task, TaskUpdateRequestDTO taskUpdateRequestDTO) {
        if(taskUpdateRequestDTO.title() != null && !taskUpdateRequestDTO.title().isBlank()) task.setTitle(taskUpdateRequestDTO.title());
        if(taskUpdateRequestDTO.description() != null) task.setDescription(taskUpdateRequestDTO.description());
        if(taskUpdateRequestDTO.dueDate() != null) task.setDueDate(taskUpdateRequestDTO.dueDate());
    }

}
