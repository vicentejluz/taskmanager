package com.vicente.taskmanager.service;

import com.vicente.taskmanager.model.dto.TaskRequestDTO;
import com.vicente.taskmanager.model.dto.TaskResponseDTO;

import java.util.List;

public interface TaskService {
    TaskResponseDTO create(TaskRequestDTO taskRequestDTO);
    TaskResponseDTO update(Long id, TaskRequestDTO taskRequestDTO);
    TaskResponseDTO done(Long id);
    TaskResponseDTO cancel(Long id);
    TaskResponseDTO findById(Long id);
    List<TaskResponseDTO> findAll();
    List<TaskResponseDTO> findByStatus(String status);
    void updateOverdueTasks();
}
