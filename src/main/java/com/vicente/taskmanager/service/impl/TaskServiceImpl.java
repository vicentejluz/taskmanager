package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.exception.TaskNotFoundException;
import com.vicente.taskmanager.exception.TaskStatusNotAllowedException;
import com.vicente.taskmanager.mapper.TaskMapper;
import com.vicente.taskmanager.model.entity.Task;
import com.vicente.taskmanager.model.enums.TaskStatus;
import com.vicente.taskmanager.dto.response.PageResponseDTO;
import com.vicente.taskmanager.dto.request.TaskCreateRequestDTO;
import com.vicente.taskmanager.dto.response.TaskResponseDTO;
import com.vicente.taskmanager.dto.request.TaskUpdateRequestDTO;
import com.vicente.taskmanager.model.entity.User;
import com.vicente.taskmanager.repository.TaskRepository;
import com.vicente.taskmanager.dto.filter.TaskFilterDTO;
import com.vicente.taskmanager.repository.specification.TaskSpecification;
import com.vicente.taskmanager.service.TaskService;
import jakarta.persistence.EntityManager;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Pageable;
import java.time.LocalDate;

@Service
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;
    private final EntityManager entityManager;
    private static final Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);

    public TaskServiceImpl(TaskRepository taskRepository, EntityManager entityManager) {
        this.taskRepository = taskRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public TaskResponseDTO create(TaskCreateRequestDTO taskCreateRequestDTO, User user) {
        logger.info("Starting create task");
        Task task = TaskMapper.toEntity(taskCreateRequestDTO, user);

        task = taskRepository.save(task);
        entityManager.refresh(task);

        logger.info("Task created successfully | taskId={} userId={}", task.getId(), user.getId());
        return TaskMapper.toDTO(task);
    }

    @Override
    @Transactional
    public TaskResponseDTO update(Long id, Long userId, TaskUpdateRequestDTO taskUpdateRequestDTO) {
        logger.info("Starting update task | taskId={} userId={}", id, userId);

        Task task = findByIdAndUserId(id, userId);

        if(task.getStatus().equals(TaskStatus.CANCELLED) || task.getStatus().equals(TaskStatus.DONE)) {
            logger.debug("Update not allowed for task id={} with status={} | userId={}", id, task.getStatus(), userId);
            throw new TaskStatusNotAllowedException("Task with status DONE or CANCELLED cannot be updated");
        }

        TaskStatus previousStatus = task.getStatus();

        TaskMapper.merge(task, taskUpdateRequestDTO);

        LocalDate today = LocalDate.now();

        if(task.getDueDate().isBefore(today) && previousStatus.equals(TaskStatus.IN_PROGRESS)) {
            task.setStatus(TaskStatus.PENDING);
        }
        else if(!task.getDueDate().isBefore(today) &&
                previousStatus.equals(TaskStatus.PENDING)) {
            task.setStatus(TaskStatus.IN_PROGRESS);
        }
        logTaskStatusChange(task, userId, previousStatus);

        saveAndFlushAndRefresh(task);

        logger.info("Task updated successfully | taskId={} userId={}", id,  userId);

        return TaskMapper.toDTO(task);
    }

    @Override
    @Transactional
    public TaskResponseDTO done(Long id, Long userId) {
        logger.info("Starting done task | taskId={} userId={}", id, userId);
        Task task = findByIdAndUserId(id, userId);

        if(task.getStatus() != TaskStatus.IN_PROGRESS) {
            logger.debug("Task status transition not allowed | taskId={} userId={} currentStatus={} attemptedStatus=DONE",
                    task.getId(), userId, task.getStatus());
            throw new TaskStatusNotAllowedException("Only tasks with status IN_PROGRESS can be marked as DONE");
        }

        TaskStatus previousStatus = task.getStatus();
        task.setStatus(TaskStatus.DONE);
        logTaskStatusChange(task, userId, previousStatus);

        saveAndFlushAndRefresh(task);

        logger.info("Task marked as DONE successfully | taskId={} userId={}", task.getId(), userId);

        return TaskMapper.toDTO(task);
    }

    @Override
    @Transactional
    public TaskResponseDTO cancel(Long id, Long userId) {
        logger.info("Starting cancel task | taskId={} userId={}", id, userId);

        Task task = findByIdAndUserId(id, userId);

        if(task.getStatus() != TaskStatus.IN_PROGRESS && task.getStatus() != TaskStatus.PENDING) {
            logger.debug("Task status transition not allowed | taskId={} userId={}" +
                            " currentStatus={} attemptedStatus=Cancelled", userId, task.getId(), task.getStatus());
            throw new TaskStatusNotAllowedException("Only tasks with status IN_PROGRESS or PENDING can be cancelled");
        }

        TaskStatus previousStatus = task.getStatus();
        task.setStatus(TaskStatus.CANCELLED);
        logTaskStatusChange(task, userId, previousStatus);

        saveAndFlushAndRefresh(task);

        logger.info("Task CANCELLED successfully | taskId={} userId={}", task.getId(), userId);

        return TaskMapper.toDTO(task);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponseDTO findById(Long id, Long userId){
        logger.info("Starting find by id task | taskId={} userId={}", id, userId);

        Task task = findByIdAndUserId(id, userId);

        logger.info("Task found successfully | taskId={} userId={}", task.getId(), userId);

        return TaskMapper.toDTO(task);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponseDTO findById(Long id) {
        logger.info("Starting find by id task | taskId={}", id);
        Task task = taskRepository.findById(id).orElseThrow(() ->
                new TaskNotFoundException("Task not found with id: " + id));

        logger.info("Task found successfully | taskId={}", task.getId());
        return TaskMapper.toDTO(task);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<TaskResponseDTO> find(String status, LocalDate dueDate, Long userId, Pageable pageable) {
        logger.info("Starting find tasks with status {} and dueDate {}", status, dueDate);

        pageable = sortPageable(pageable);

        logTaskFindStrategy(status, dueDate);

        Specification<Task> spec = TaskSpecification.filter(new TaskFilterDTO(userId,
                TaskStatus.converter(status), dueDate));
        Page<TaskResponseDTO> tasks = findAll(spec, pageable);

        logger.info("Find tasks success | totalElements={} totalPages={} page={} size={}", tasks.getTotalElements(),
                tasks.getTotalPages(), pageable.getPageNumber(), pageable.getPageSize());

        return TaskMapper.toPageDTO(tasks);
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        logger.info("Starting delete task | taskId={}", id);
        Task task = taskRepository.findById(id).orElseThrow(() ->
                new TaskNotFoundException("Task not found with id: " + id));

        taskRepository.delete(task);

        logger.info("Task deleted successfully | taskId={}", task.getId());
    }

    private static @NonNull Pageable sortPageable(Pageable pageable) {
        return (pageable.getSort().isUnsorted()) ? PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("status").ascending().
                        and(Sort.by("dueDate").ascending())) : pageable;
    }

    private void saveAndFlushAndRefresh(Task task) {
        taskRepository.saveAndFlush(task);
        entityManager.refresh(task);
    }

    private @NonNull Task findByIdAndUserId(Long id, Long userId) {
        return taskRepository.findByIdAndUserId(id, userId).orElseThrow(() ->
                new TaskNotFoundException("Task not found or you do not have permission to access it"));
    }

    private Page<TaskResponseDTO> findAll(Specification<Task> spec, Pageable pageable){
        return taskRepository.findAll(spec, pageable).map(TaskMapper::toDTO);
    }

    private void logTaskStatusChange(Task task, Long userId, TaskStatus previousStatus) {
        logger.info("Task status changed | taskId={} userId={} from={} to={}", task.getId(), userId,
                previousStatus, task.getStatus());
    }

    private void logTaskFindStrategy(String status, LocalDate dueDate) {
        if((status != null && !status.isBlank())  && dueDate != null) {
            logger.debug("Find strategy: status + dueDate | status={} dueDate={}", status, dueDate);
        } else if(status != null && !status.isBlank()) {
            logger.debug("Find strategy: status | status={}", status);
        } else if(dueDate != null) {
            logger.debug("Find strategy: dueDate | dueDate={}", dueDate);
        } else {
            logger.debug("Find strategy: all tasks");
        }
    }
}
