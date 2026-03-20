package com.vicente.taskmanager.scheduler;

import com.vicente.taskmanager.service.VerificationTokenSchedulerService;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VerificationTokenScheduler {
    private final VerificationTokenSchedulerService verificationTokenSchedulerService;
    private static final Logger logger = LoggerFactory.getLogger(VerificationTokenScheduler.class);

    public VerificationTokenScheduler(VerificationTokenSchedulerService verificationTokenSchedulerService) {
        this.verificationTokenSchedulerService = verificationTokenSchedulerService;
    }

    @Scheduled(cron = "${spring.verification.token.scheduling.cron}")
    public void runScheduled() {
        execute();
    }

    private void execute() {
        logger.info("[VERIFICATION TOKEN SCHEDULER] Running task maintenance");

        long start = System.currentTimeMillis();
        try {
            verificationTokenSchedulerService.deleteVerificationTokenTypeEmailExpiredBefore2Days();
            verificationTokenSchedulerService.deleteVerificationTokenTypePasswordExpiredBefore1Days();

            long duration = System.currentTimeMillis() - start;
            logger.info("[VERIFICATION TOKEN SCHEDULER] Verification token maintenance scheduler finished | duration={}ms", duration);

        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
            logger.warn("[VERIFICATION TOKEN SCHEDULER] Verification token skipped due to concurrent update | reason=optimistic_lock");
        } catch (Exception e){
            logger.error("[VERIFICATION TOKEN SCHEDULER] Verification token maintenance scheduler failed due to unexpected error", e);
        }
    }
}
