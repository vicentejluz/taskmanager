package com.vicente.taskmanager.scheduler;

import com.vicente.taskmanager.service.TaskSchedulerService;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component(value = "TaskMaintenanceScheduler")
public class TaskScheduler {
    private final TaskSchedulerService taskSchedulerService;

    public TaskScheduler(TaskSchedulerService taskSchedulerService) {
        this.taskSchedulerService = taskSchedulerService;
    }

    @PostConstruct
    @Scheduled(cron = "${spring.task.scheduling.cron}")
    public void execute() {
        if(taskSchedulerService.hasTasks()) {
            taskSchedulerService.updateOverdueTasks();
            taskSchedulerService.deleteCancelledTasksOlderThan90Days();
            taskSchedulerService.deleteDoneTasksOlderThan180Days();
        }
    }
}
