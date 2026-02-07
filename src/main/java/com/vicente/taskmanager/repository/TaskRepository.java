package com.vicente.taskmanager.repository;

import com.vicente.taskmanager.model.domain.Task;
import com.vicente.taskmanager.model.domain.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatus(TaskStatus status);
    Page<Task> findByStatus(TaskStatus status, Pageable pageable);
    Page<Task> findByDueDate(LocalDate dueDate, Pageable pageable);
    Page<Task> findByStatusAndDueDate(TaskStatus status, LocalDate dueDate, Pageable pageable);
}
