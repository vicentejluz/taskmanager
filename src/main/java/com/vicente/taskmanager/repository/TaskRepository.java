package com.vicente.taskmanager.repository;

import com.vicente.taskmanager.model.domain.Task;
import com.vicente.taskmanager.model.domain.TaskStatus;
import com.vicente.taskmanager.model.dto.TaskResponseDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatus(TaskStatus status);
}
