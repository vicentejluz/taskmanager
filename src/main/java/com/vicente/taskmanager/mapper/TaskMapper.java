package com.vicente.taskmanager.mapper;

import com.vicente.taskmanager.model.entity.Task;
import com.vicente.taskmanager.dto.response.PageResponseDTO;
import com.vicente.taskmanager.dto.request.TaskCreateRequestDTO;
import com.vicente.taskmanager.dto.response.TaskResponseDTO;
import com.vicente.taskmanager.dto.request.TaskUpdateRequestDTO;
import com.vicente.taskmanager.model.entity.User;
import org.springframework.data.domain.Page;

public final class TaskMapper {

    public static Task toEntity(TaskCreateRequestDTO request, User user) {
        return new Task(request.title(), request.description(), request.dueDate(), user);
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

    public static PageResponseDTO<TaskResponseDTO> toPageDTO(Page<Task> page) {
        Page<TaskResponseDTO> pageDTO = page.map(TaskMapper::toDTO);
        return new PageResponseDTO<>(
                pageDTO.getContent(),
                pageDTO.getNumber(),
                pageDTO.getSize(),
                pageDTO.getTotalPages(),
                pageDTO.getTotalElements(),
                pageDTO.isFirst(),
                pageDTO.isLast()
        );
    }

    public static void merge(Task task, TaskUpdateRequestDTO taskUpdateRequestDTO) {
        if(taskUpdateRequestDTO.title() != null && !taskUpdateRequestDTO.title().isBlank())
            task.setTitle(taskUpdateRequestDTO.title());
        if(taskUpdateRequestDTO.description() != null)
            task.setDescription(taskUpdateRequestDTO.description());
        if(taskUpdateRequestDTO.dueDate() != null)
            task.setDueDate(taskUpdateRequestDTO.dueDate());
    }

}
