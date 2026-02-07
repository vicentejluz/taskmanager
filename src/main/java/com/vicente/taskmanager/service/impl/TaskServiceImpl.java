package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.exception.TaskStatusNotAllowedException;
import com.vicente.taskmanager.mapper.TaskMapper;
import com.vicente.taskmanager.model.domain.Task;
import com.vicente.taskmanager.model.domain.TaskStatus;
import com.vicente.taskmanager.model.dto.PageResponseDTO;
import com.vicente.taskmanager.model.dto.TaskCreateRequestDTO;
import com.vicente.taskmanager.model.dto.TaskResponseDTO;
import com.vicente.taskmanager.model.dto.TaskUpdateRequestDTO;
import com.vicente.taskmanager.repository.TaskRepository;
import com.vicente.taskmanager.service.TaskService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Pageable;
import java.time.LocalDate;

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
    public TaskResponseDTO create(TaskCreateRequestDTO taskCreateRequestDTO) {
        Task task = TaskMapper.toEntity(taskCreateRequestDTO);

        task = taskRepository.save(task);
        entityManager.refresh(task);

        return TaskMapper.toDTO(task);
    }

    @Override
    @Transactional
    public TaskResponseDTO update(Long id, TaskUpdateRequestDTO taskUpdateRequestDTO) {
        Task task = taskRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Task not found with id: " + id));

        if(task.getStatus().equals(TaskStatus.CANCELLED) || task.getStatus().equals(TaskStatus.DONE))
            throw new TaskStatusNotAllowedException("Task with status DONE or CANCELLED cannot be updated");

        TaskMapper.merge(task, taskUpdateRequestDTO);

        if(task.getDueDate().equals(taskUpdateRequestDTO.dueDate())){
            task.setStatus(TaskStatus.IN_PROGRESS);
        }

        saveAndFlushAndRefresh(task);

        return TaskMapper.toDTO(task);
    }

    @Override
    @Transactional
    public TaskResponseDTO done(Long id) {
        Task task = taskRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Task not found with id: " + id));

        if(task.getStatus() != TaskStatus.IN_PROGRESS)
            throw new TaskStatusNotAllowedException("Only tasks with status IN_PROGRESS can be marked as DONE");

        task.setStatus(TaskStatus.DONE);

        saveAndFlushAndRefresh(task);

        return TaskMapper.toDTO(task);
    }

    @Override
    @Transactional
    public TaskResponseDTO cancel(Long id) {
        Task task = taskRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Task not found with id: " + id));

        if(task.getStatus() != TaskStatus.IN_PROGRESS && task.getStatus() != TaskStatus.PENDING)
            throw new TaskStatusNotAllowedException("Only tasks with status IN_PROGRESS or PENDING can be cancelled");

        task.setStatus(TaskStatus.CANCELLED);

        saveAndFlushAndRefresh(task);

        return TaskMapper.toDTO(task);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponseDTO findById(Long id){
        Task task = taskRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Task not found with id: " + id));

        return TaskMapper.toDTO(task);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<TaskResponseDTO> find(String status, LocalDate dueDate, Pageable pageable) {

            if((status != null && !status.isBlank())  && dueDate != null) {
                return findByStatusAndDueDate(status, dueDate, pageable);
            }

            if(status != null && !status.isBlank()){
                return findByStatus(status, pageable);
            }

            if(dueDate != null){
                return findByDueDate(dueDate, pageable);
            }

            return findAll(pageable);
    }

    private void saveAndFlushAndRefresh(Task task) {
        taskRepository.saveAndFlush(task);
        entityManager.refresh(task);
    }

    private PageResponseDTO<TaskResponseDTO>findByStatusAndDueDate(String status, LocalDate dueDate, Pageable pageable){
        Page<TaskResponseDTO> tasks = taskRepository.findByStatusAndDueDate(
                TaskStatus.converter(status), dueDate, pageable).map(TaskMapper::toDTO);

        return TaskMapper.toPageDTO(tasks);
    }

    private PageResponseDTO<TaskResponseDTO>findByStatus(String status, Pageable pageable){
        Page<TaskResponseDTO> tasks = taskRepository.findByStatus(TaskStatus.converter(status), pageable)
                .map(TaskMapper::toDTO);

        return TaskMapper.toPageDTO(tasks);
    }

    private PageResponseDTO<TaskResponseDTO>findByDueDate(LocalDate dueDate, Pageable pageable){
        Page<TaskResponseDTO> tasks = taskRepository.findByDueDate(dueDate, pageable).map(TaskMapper::toDTO);

        return TaskMapper.toPageDTO(tasks);
    }

    private PageResponseDTO<TaskResponseDTO>findAll(Pageable pageable){
        Page<TaskResponseDTO> tasks = taskRepository.findAll(pageable).map(TaskMapper::toDTO);

        return TaskMapper.toPageDTO(tasks);
    }
}
