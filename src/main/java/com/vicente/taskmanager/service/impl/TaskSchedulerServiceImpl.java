package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.model.domain.Task;
import com.vicente.taskmanager.model.domain.TaskStatus;
import com.vicente.taskmanager.repository.TaskRepository;
import com.vicente.taskmanager.service.TaskSchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class TaskSchedulerServiceImpl implements TaskSchedulerService {

    private final TaskRepository taskRepository;
    private static final Logger logger = LoggerFactory.getLogger(TaskSchedulerServiceImpl.class);

    public TaskSchedulerServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
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
            logger.info("[SCHEDULER] Overdue tasks updated | count={}", overdueTasks.size());
        }
        else {
            logger.debug("[SCHEDULER] No overdue tasks found");
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
            logger.info("[SCHEDULER] tasks deleted | status={} olderThan={}days count={}", taskStatus, qtdDay, tasks.size());
        }else{
            logger.debug("[SCHEDULER] No tasks to delete | status={} olderThan={}days", taskStatus, qtdDay);
        }
    }
}
