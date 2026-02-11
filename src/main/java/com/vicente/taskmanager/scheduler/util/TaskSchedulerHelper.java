package com.vicente.taskmanager.scheduler.util;

import com.vicente.taskmanager.model.domain.Task;
import com.vicente.taskmanager.model.domain.TaskStatus;
import com.vicente.taskmanager.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TaskSchedulerHelper {
    private final TaskRepository taskRepository;
    private final Logger logger = LoggerFactory.getLogger(TaskSchedulerHelper.class);

    public TaskSchedulerHelper(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteSingleTask(String source, Task task) {
        logger.debug("[{}] Executing Delete single task | taskId={}", source, task.getId());
        taskRepository.delete(task);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSingleTask(String source, Task task) {
        logger.debug("[{}] Executing Save single task | taskId={}", source, task.getId());
        task.setStatus(TaskStatus.PENDING);
        taskRepository.save(task);
    }
}
