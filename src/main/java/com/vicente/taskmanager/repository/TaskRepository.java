package com.vicente.taskmanager.repository;

import com.vicente.taskmanager.model.entity.Task;
import com.vicente.taskmanager.model.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    List<Task> findByStatusAndDueDateBefore(TaskStatus status, LocalDate today);

    List<Task> findByStatusAndUpdatedAtBefore(TaskStatus status, OffsetDateTime thresholdDate);

    Optional<Task> findByIdAndUserId(Long id, Long userId);
}