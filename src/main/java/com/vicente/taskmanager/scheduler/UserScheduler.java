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
    private static final String EVERY_DAY = "EVERY DAY";
    private static final String EVERY_HOUR = "EVERY HOUR";

    public UserScheduler(UserSchedulerService userSchedulerService) {
        this.userSchedulerService = userSchedulerService;
    }

    @Scheduled(cron = "${spring.user.scheduling.cron.every.day}")
    public void runScheduledEveryDay() {
        execute(EVERY_DAY);
    }

    @Scheduled(cron = "${spring.user.scheduling.cron.every.hour}")
    public void runScheduledEveryHour() {
        execute(EVERY_HOUR);
    }

    private void execute(String source) {
        logger.info("[USER SCHEDULER {}] Running task maintenance",  source);

        long start = System.currentTimeMillis();

        try {
            if(source.equals(EVERY_DAY)) {
                userSchedulerService.deleteDisabledUsersOlderThan180Days();
                userSchedulerService.deleteUsersWithDeletedAtOlderThan180Days();
                userSchedulerService.unlockUsersWithExpiredLock();
            }else{
                userSchedulerService.deleteUsersWithPendingVerificationOlderThan72Hours();
            }
            long duration = System.currentTimeMillis() - start;
            logger.info("[USER SCHEDULER {}] User scheduler finished | duration={}ms",source, duration);

        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
            logger.warn("[USER SCHEDULER {}] User skipped due to concurrent | reason=optimistic_lock", source);
        } catch (Exception e){
            logger.error("[USER SCHEDULER {}] User scheduler failed due to unexpected error", source, e);
        }
    }
}
