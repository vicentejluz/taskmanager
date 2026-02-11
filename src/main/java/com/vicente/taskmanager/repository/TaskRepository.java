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

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatusAndDueDateBefore(TaskStatus status, LocalDate today);
    List<Task> findByStatusAndUpdatedAtBefore(TaskStatus status, OffsetDateTime thresholdDate);
    Page<Task> findByStatus(TaskStatus status, Pageable pageable);
    Page<Task> findByDueDate(LocalDate dueDate, Pageable pageable);
    Page<Task> findByStatusAndDueDate(TaskStatus status, LocalDate dueDate, Pageable pageable);
}
