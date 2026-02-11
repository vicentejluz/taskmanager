package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.model.entity.Task;
import com.vicente.taskmanager.model.entity.TaskStatus;
import com.vicente.taskmanager.repository.TaskRepository;
import com.vicente.taskmanager.scheduler.util.TaskSchedulerHelper;
import com.vicente.taskmanager.service.TaskSchedulerService;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TaskSchedulerServiceImpl implements TaskSchedulerService {

    private final TaskRepository taskRepository;
    private final TaskSchedulerHelper taskSchedulerHelper;
    private static final Logger logger = LoggerFactory.getLogger(TaskSchedulerServiceImpl.class);

    public TaskSchedulerServiceImpl(TaskRepository taskRepository, TaskSchedulerHelper taskSchedulerHelper) {
        this.taskRepository = taskRepository;
        this.taskSchedulerHelper = taskSchedulerHelper;
    }

    @Override
    @Transactional(readOnly = true)
    public void updateOverdueTasks(String source){
        LocalDate today = LocalDate.now();
        List<Task> overdueTasks = taskRepository.findByStatusAndDueDateBefore(TaskStatus.IN_PROGRESS, today);

        if (!overdueTasks.isEmpty()) {
            AtomicInteger count = new AtomicInteger();
            overdueTasks.forEach(task -> {
                try {
                    taskSchedulerHelper.saveSingleTask(source, task);
                    count.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                    logger.warn("[{}] Task skipped due to optimistic lock - updateOverdueTasks | taskId={}",
                            source, task.getId());
                }
            });
            if(count.get() > 0) {
                logger.info("[{}] Overdue tasks updated | count={}", source, count.get());
                return;
            }
            logger.info("[{}] Overdue tasks found but none updated due to concurrency",  source);
        }
        else {
            logger.debug("[{}] No overdue tasks found", source);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void deleteCancelledTasksOlderThan90Days(String source) {
        deleteTasksByStatusOlderThan(source, TaskStatus.CANCELLED, 90);
    }

    @Override
    @Transactional(readOnly = true)
    public void deleteDoneTasksOlderThan180Days(String source) {
        deleteTasksByStatusOlderThan(source, TaskStatus.DONE, 180);
    }


    private void deleteTasksByStatusOlderThan(String source, TaskStatus taskStatus, int qtdDay){
        OffsetDateTime thresholdDate = OffsetDateTime.now().minusDays(qtdDay);
        List<Task> tasks = taskRepository.findByStatusAndUpdatedAtBefore(taskStatus, thresholdDate);

        if (!tasks.isEmpty()) {
            AtomicInteger count = new AtomicInteger();
            tasks.forEach(task -> {
                try {
                    taskSchedulerHelper.deleteSingleTask(source, task);
                    count.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                    logger.warn("[{}] Task skipped due to optimistic lock - deleteTasksByStatusOlderThan | taskId={}",
                            source, task.getId());
                }
            });
            if(count.get() > 0) {
                logger.info("[{}] tasks deleted | status={} olderThan={}days count={}",
                        source, taskStatus, qtdDay, count.get());
                return;
            }
            logger.info("[{}] Overdue tasks found but none cancelled due to concurrency",  source);
        }else{
            logger.debug("[{}] No tasks to delete | status={} olderThan={}days", source, taskStatus, qtdDay);
        }
    }
}
