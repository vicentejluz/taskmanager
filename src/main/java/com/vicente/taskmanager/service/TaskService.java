package com.vicente.taskmanager.service;

import com.vicente.taskmanager.model.dto.PageResponseDTO;
import com.vicente.taskmanager.model.dto.TaskCreateRequestDTO;
import com.vicente.taskmanager.model.dto.TaskResponseDTO;
import com.vicente.taskmanager.model.dto.TaskUpdateRequestDTO;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;

public interface TaskService {
    TaskResponseDTO create(TaskCreateRequestDTO taskCreateRequestDTO);
    TaskResponseDTO update(Long id, TaskUpdateRequestDTO TaskUpdateRequestDTO);
    TaskResponseDTO done(Long id);
    TaskResponseDTO cancel(Long id);
    TaskResponseDTO findById(Long id);
    PageResponseDTO<TaskResponseDTO> find(String status, LocalDate dueDate, Pageable pageable);
}
