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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Pageable;
import java.time.LocalDate;

@Service
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;
    private final EntityManager entityManager;
    private static final Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);

    public TaskServiceImpl(TaskRepository taskRepository,  EntityManager entityManager) {
        this.taskRepository = taskRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public TaskResponseDTO create(TaskCreateRequestDTO taskCreateRequestDTO) {
        logger.info("Starting create task");
        Task task = TaskMapper.toEntity(taskCreateRequestDTO);

        task = taskRepository.save(task);
        entityManager.refresh(task);

        logger.info("Task created successfully | taskId={}", task.getId());

        return TaskMapper.toDTO(task);
    }

    @Override
    @Transactional
    public TaskResponseDTO update(Long id, TaskUpdateRequestDTO taskUpdateRequestDTO) {
        logger.info("Starting update task with id {}", id);

        Task task = taskRepository.findById(id).orElseThrow(() ->
            new EntityNotFoundException("Task not found with id: " + id));

        if(task.getStatus().equals(TaskStatus.CANCELLED) || task.getStatus().equals(TaskStatus.DONE)) {
            logger.debug("Update not allowed for task id={} with status={}", id, task.getStatus());
            throw new TaskStatusNotAllowedException("Task with status DONE or CANCELLED cannot be updated");
        }

        TaskStatus previousStatus = task.getStatus();

        TaskMapper.merge(task, taskUpdateRequestDTO);

        if(task.getDueDate().equals(taskUpdateRequestDTO.dueDate()) && task.getStatus().equals(TaskStatus.PENDING)) {
            task.setStatus(TaskStatus.IN_PROGRESS);
            logTaskStatusChange(task, previousStatus);
        }

        saveAndFlushAndRefresh(task);

        logger.info("Task updated successfully | taskId={}", id);

        return TaskMapper.toDTO(task);
    }

    @Override
    @Transactional
    public TaskResponseDTO done(Long id) {
        logger.info("Starting done task with id {}", id);
        Task task = taskRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Task not found with id: " + id));

        if(task.getStatus() != TaskStatus.IN_PROGRESS) {
            logger.debug("Task status transition not allowed | taskId={} currentStatus={} attemptedStatus=DONE",
                    task.getId(), task.getStatus());
            throw new TaskStatusNotAllowedException("Only tasks with status IN_PROGRESS can be marked as DONE");
        }

        TaskStatus previousStatus = task.getStatus();
        task.setStatus(TaskStatus.DONE);
        logTaskStatusChange(task, previousStatus);

        saveAndFlushAndRefresh(task);

        logger.info("Task marked as DONE successfully | taskId={}", task.getId());

        return TaskMapper.toDTO(task);
    }

    @Override
    @Transactional
    public TaskResponseDTO cancel(Long id) {
        logger.info("Starting cancel task with id {}", id);

        Task task = taskRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Task not found with id: " + id));

        if(task.getStatus() != TaskStatus.IN_PROGRESS && task.getStatus() != TaskStatus.PENDING) {
            logger.debug("Task status transition not allowed | taskId={} currentStatus={} attemptedStatus=Cancelled",
                    task.getId(), task.getStatus());
            throw new TaskStatusNotAllowedException("Only tasks with status IN_PROGRESS or PENDING can be cancelled");
        }

        TaskStatus previousStatus = task.getStatus();
        task.setStatus(TaskStatus.CANCELLED);
        logTaskStatusChange(task, previousStatus);

        saveAndFlushAndRefresh(task);

        logger.info("Task CANCELLED successfully | taskId={}", task.getId());

        return TaskMapper.toDTO(task);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponseDTO findById(Long id){
        logger.info("Starting find task with id {}", id);

        Task task = taskRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Task not found with id: " + id));

        logger.info("Task found successfully | taskId={}", task.getId());

        return TaskMapper.toDTO(task);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<TaskResponseDTO> find(String status, LocalDate dueDate, Pageable pageable) {
        logger.info("Starting find tasks with status {} and dueDate {}", status, dueDate);

        Page<TaskResponseDTO> tasks;

        if((status != null && !status.isBlank())  && dueDate != null) {
            logger.debug("Find strategy: status + dueDate | status={} dueDate={}", status, dueDate);
            tasks = findByStatusAndDueDate(status, dueDate, pageable);
        } else if(status != null && !status.isBlank()) {
            logger.debug("Find strategy: status | status={}", status);
            tasks = findByStatus(status, pageable);
        } else if(dueDate != null) {
            logger.debug("Find strategy: dueDate | dueDate={}", dueDate);
            tasks = findByDueDate(dueDate, pageable);
        } else {
            logger.debug("Find strategy: all tasks");
            tasks = findAll(pageable);
        }

        logger.info("Find tasks success | totalElements={} totalPages={} page={} size={}", tasks.getTotalElements(),
                tasks.getTotalPages(), pageable.getPageNumber(), pageable.getPageSize());

        return TaskMapper.toPageDTO(tasks);
    }

    private void saveAndFlushAndRefresh(Task task) {
        taskRepository.saveAndFlush(task);
        entityManager.refresh(task);
    }

    private Page<TaskResponseDTO> findByStatusAndDueDate(String status, LocalDate dueDate, Pageable pageable){
         return taskRepository.findByStatusAndDueDate(TaskStatus.converter(status), dueDate, pageable)
                 .map(TaskMapper::toDTO);
    }

    private Page<TaskResponseDTO> findByStatus(String status, Pageable pageable){
        return taskRepository.findByStatus(TaskStatus.converter(status), pageable)
                .map(TaskMapper::toDTO);
    }

    private Page<TaskResponseDTO> findByDueDate(LocalDate dueDate, Pageable pageable){
        return taskRepository.findByDueDate(dueDate, pageable).map(TaskMapper::toDTO);
    }

    private Page<TaskResponseDTO> findAll(Pageable pageable){
        return taskRepository.findAll(pageable).map(TaskMapper::toDTO);
    }

    private void logTaskStatusChange(Task task, TaskStatus previousStatus) {
        logger.info("Task status changed | taskId={} from={} to={}", task.getId(),
                previousStatus, task.getStatus());
    }
}
