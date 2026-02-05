package com.vicente.taskmanager.mapper;

import com.vicente.taskmanager.model.domain.Task;
import com.vicente.taskmanager.model.dto.TaskRequestDTO;
import com.vicente.taskmanager.model.dto.TaskResponseDTO;

public final class TaskMapper {

    public static Task toEntity(TaskRequestDTO request) {
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

    public static void merge(Task task, TaskRequestDTO taskRequestDTO) {
        if(taskRequestDTO.title() != null) task.setTitle(taskRequestDTO.title());
        if(taskRequestDTO.description() != null) task.setDescription(taskRequestDTO.description());
        if(taskRequestDTO.dueDate() != null) task.setDueDate(taskRequestDTO.dueDate());
    }

}
