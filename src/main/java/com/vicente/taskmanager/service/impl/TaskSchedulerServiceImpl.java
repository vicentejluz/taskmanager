package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.model.domain.Task;
import com.vicente.taskmanager.model.domain.TaskStatus;
import com.vicente.taskmanager.repository.TaskRepository;
import com.vicente.taskmanager.service.TaskSchedulerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class TaskSchedulerServiceImpl implements TaskSchedulerService {

    private final TaskRepository taskRepository;

    public TaskSchedulerServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasTasks(){
        return taskRepository.count() > 0;
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

    @Override
    @Transactional
    public void deleteCancelledTasksOlderThan90Days() {
        deleteTasksByStatusOlderThan(TaskStatus.CANCELLED, 90);
    }

    @Override
    @Transactional
    public void deleteDoneTasksOlderThan180Days() {
        deleteTasksByStatusOlderThan(TaskStatus.DONE, 180);
    }

    private void deleteTasksByStatusOlderThan(TaskStatus taskStatus, int qtdDay){
        List<Task> tasks = taskRepository.findByStatus(taskStatus).stream()
                .filter(task -> task.getUpdatedAt().isBefore(
                        OffsetDateTime.now().minusDays(qtdDay))).toList();

        if (!tasks.isEmpty()) {
            taskRepository.deleteAll(tasks);
        }
    }
}
