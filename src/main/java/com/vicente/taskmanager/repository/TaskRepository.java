package com.vicente.taskmanager.repository;

import com.vicente.taskmanager.model.entity.Task;
import com.vicente.taskmanager.model.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatusAndDueDateBefore(TaskStatus status, LocalDate today);

    List<Task> findByStatusAndUpdatedAtBefore(TaskStatus status, OffsetDateTime thresholdDate);

    Page<Task> findByStatusAndUserId(TaskStatus status, Long userId, Pageable pageable);

    Page<Task> findByDueDateAndUserId(LocalDate dueDate, Long userId, Pageable pageable);

    Page<Task> findByStatusAndDueDateAndUserId(TaskStatus status, LocalDate dueDate, Long userId, Pageable pageable);

    Page<Task> findAllByUserId(Long userId, Pageable pageable);

    Optional<Task> findByIdAndUserId(Long id, Long userId);
}