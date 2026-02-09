package com.vicente.taskmanager.scheduler;

import com.vicente.taskmanager.service.TaskSchedulerService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component(value = "TaskMaintenanceScheduler")
public class TaskScheduler {
    private final TaskSchedulerService taskSchedulerService;
    private static final Logger logger = LoggerFactory.getLogger(TaskScheduler.class);

    public TaskScheduler(TaskSchedulerService taskSchedulerService) {
        this.taskSchedulerService = taskSchedulerService;
    }

    @PostConstruct
    @Scheduled(cron = "${spring.task.scheduling.cron}")
    public void execute() {
        long start = System.currentTimeMillis();
        logger.info("[SCHEDULER] Executing Task maintenance scheduler");

        try {
            taskSchedulerService.updateOverdueTasks();
            taskSchedulerService.deleteCancelledTasksOlderThan90Days();
            taskSchedulerService.deleteDoneTasksOlderThan180Days();

            long duration = System.currentTimeMillis() - start;
            logger.info("[SCHEDULER] Task maintenance scheduler finished | duration={}ms", duration);
        }catch (Exception e){
            logger.error("[SCHEDULER] Task maintenance scheduler failed", e);
            throw e;
        }
    }
}
