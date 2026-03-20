package com.vicente.taskmanager.scheduler;

import com.vicente.taskmanager.service.RefreshTokenSchedulerService;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenScheduler {
    private final RefreshTokenSchedulerService refreshTokenSchedulerService;
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenScheduler.class);

    public RefreshTokenScheduler(RefreshTokenSchedulerService refreshTokenSchedulerService) {
        this.refreshTokenSchedulerService = refreshTokenSchedulerService;
    }

    @Scheduled(cron = "${spring.refresh.token.scheduling.cron}")
    public void runScheduled() {
        execute();
    }

    private void execute() {
        logger.info("[REFRESH TOKEN SCHEDULER] Running task maintenance");

        long start = System.currentTimeMillis();
        try {
            refreshTokenSchedulerService.deleteRefreshTokensExpiredBefore7Days();
            refreshTokenSchedulerService.deleteRefreshTokensOfUsersDeletedBefore3Days();

            long duration = System.currentTimeMillis() - start;
            logger.info("[REFRESH TOKEN SCHEDULER] Refresh token maintenance scheduler finished | duration={}ms", duration);

        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
            logger.warn("[REFRESH TOKEN SCHEDULER] Refresh token skipped due to concurrent update | reason=optimistic_lock");
        } catch (Exception e){
            logger.error("[REFRESH TOKEN SCHEDULER] Refresh token maintenance scheduler failed due to unexpected error", e);
        }
    }
}
