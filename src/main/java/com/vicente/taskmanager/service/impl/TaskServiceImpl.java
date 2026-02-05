package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.mapper.TaskMapper;
import com.vicente.taskmanager.model.domain.Task;
import com.vicente.taskmanager.model.domain.TaskStatus;
import com.vicente.taskmanager.model.dto.TaskRequestDTO;
import com.vicente.taskmanager.model.dto.TaskResponseDTO;
import com.vicente.taskmanager.repository.TaskRepository;
import com.vicente.taskmanager.service.TaskService;
import com.vicente.taskmanager.service.exception.BusinessRuleViolationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;
    private final EntityManager entityManager;

    public TaskServiceImpl(TaskRepository taskRepository,  EntityManager entityManager) {
        this.taskRepository = taskRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public TaskResponseDTO create(TaskRequestDTO taskRequestDTO) {
        Task task = TaskMapper.toEntity(taskRequestDTO);

        task = taskRepository.save(task);
        entityManager.refresh(task);

        return TaskMapper.toDTO(task);
    }

    @Override
    @Transactional
    public TaskResponseDTO update(Long id, TaskRequestDTO taskRequestDTO) {
        Task task = taskRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Task not found"));

        if(task.getStatus().equals(TaskStatus.CANCELLED) || task.getStatus().equals(TaskStatus.DONE))
            throw new BusinessRuleViolationException("Task with status DONE or CANCELLED cannot be updated");

        if(taskRequestDTO.dueDate() != null)
            validateDueDateIsNotInThePast(taskRequestDTO.dueDate());

        TaskMapper.merge(task, taskRequestDTO);

        task = taskRepository.saveAndFlush(task);
        entityManager.refresh(task);

        return TaskMapper.toDTO(task);
    }

    @Override
    @Transactional
    public TaskResponseDTO done(Long id) {
        Task task = taskRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Task not found"));

        if(task.getStatus() != TaskStatus.IN_PROGRESS)
            throw new BusinessRuleViolationException("Only tasks with status IN_PROGRESS can be marked as DONE");

        task.setStatus(TaskStatus.DONE);

        taskRepository.save(task);

        return TaskMapper.toDTO(task);
    }

    @Override
    @Transactional
    public TaskResponseDTO cancel(Long id) {
        Task task = taskRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Task not found"));

        if(task.getStatus() != TaskStatus.IN_PROGRESS && task.getStatus() != TaskStatus.PENDING)
            throw new BusinessRuleViolationException("Only tasks with status IN_PROGRESS or PENDING can be cancelled");

        task.setStatus(TaskStatus.CANCELLED);

        taskRepository.save(task);

        return TaskMapper.toDTO(task);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponseDTO findById(Long id){
        Task task = taskRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Task not found"));

        return TaskMapper.toDTO(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponseDTO> findAll() {
        List<Task> tasks = taskRepository.findAll();

        return tasks.stream().map(TaskMapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponseDTO> findByStatus(String status) {
        try {
            List<Task> tasks = taskRepository.findByStatus(TaskStatus.converter(status));
            return  tasks.stream().map(TaskMapper::toDTO).toList();
        }catch (IllegalArgumentException e){
            throw new BusinessRuleViolationException("Invalid task status: " + status);
        }
    }

    @Override
    @Transactional
    public void updateOverdueTasks(){
        List<Task> overdueTasks = taskRepository.findByStatus(TaskStatus.IN_PROGRESS).stream()
                .filter(task -> LocalDate.now().isAfter(task.getDueDate()))
                .map(task -> {
                    task.setStatus(TaskStatus.PENDING);
                    return task;
                }).toList();

        if (!overdueTasks.isEmpty()) {
            taskRepository.saveAll(overdueTasks);
        }
    }

    private void validateDueDateIsNotInThePast(LocalDate dueDate) {
        if(dueDate.isBefore(LocalDate.now())) {
            throw new BusinessRuleViolationException("Due date cannot be before current date");
        }
    }


}
