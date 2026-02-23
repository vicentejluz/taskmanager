package com.vicente.taskmanager.scheduler;

import com.vicente.taskmanager.service.UserSchedulerService;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UserScheduler {
    private final UserSchedulerService userSchedulerService;
    private static final Logger logger = LoggerFactory.getLogger(UserScheduler.class);

    public UserScheduler(UserSchedulerService userSchedulerService) {
        this.userSchedulerService = userSchedulerService;
    }

    @Scheduled(cron = "${spring.user.scheduling.cron}")
    public void runScheduled() {
        execute();
    }

    private void execute() {
        logger.info("[USER SCHEDULER] Running task maintenance");

        long start = System.currentTimeMillis();

        try {
            userSchedulerService.deleteDisabledUsersOlderThan180Days();
            userSchedulerService.deleteUsersWithDeleteAtOlderThan180Days();
            userSchedulerService.unlockUsersWithExpiredLock();

            long duration = System.currentTimeMillis() - start;
            logger.info("[USER SCHEDULER] User scheduler finished | duration={}ms", duration);

        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
            logger.warn("[USER SCHEDULER] User skipped due to concurrent | reason=optimistic_lock");
        } catch (Exception e){
            logger.error("[USER SCHEDULER] User scheduler failed due to unexpected error", e);
        }
    }
}
