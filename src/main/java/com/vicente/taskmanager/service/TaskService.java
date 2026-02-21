package com.vicente.taskmanager.service;

import com.vicente.taskmanager.dto.response.PageResponseDTO;
import com.vicente.taskmanager.dto.request.TaskCreateRequestDTO;
import com.vicente.taskmanager.dto.response.TaskResponseDTO;
import com.vicente.taskmanager.dto.request.TaskUpdateRequestDTO;
import com.vicente.taskmanager.model.entity.User;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface TaskService {
    TaskResponseDTO create(TaskCreateRequestDTO taskCreateRequestDTO, User user);
    TaskResponseDTO update(Long id, Long userId, TaskUpdateRequestDTO TaskUpdateRequestDTO);
    TaskResponseDTO done(Long id, Long userId);
    TaskResponseDTO cancel(Long id, Long userId);
    TaskResponseDTO findById(Long id, Long userId);
    PageResponseDTO<TaskResponseDTO> find(String status, LocalDate dueDate, Long userId, Pageable pageable);
    TaskResponseDTO findById(Long id);
    void deleteTask(Long id);

}
