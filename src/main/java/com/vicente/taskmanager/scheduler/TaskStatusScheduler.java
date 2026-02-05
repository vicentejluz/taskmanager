package com.vicente.taskmanager.scheduler;

import com.vicente.taskmanager.service.TaskService;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TaskStatusScheduler {
    TaskService taskService;

    public TaskStatusScheduler(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostConstruct
    @Scheduled(cron = "${spring.task.scheduling.cron}")
    public void updateOverdueTasks() {
        taskService.updateOverdueTasks();
    }
}
