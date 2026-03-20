package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.domain.entity.VerificationToken;
import com.vicente.taskmanager.domain.enums.TokenType;
import com.vicente.taskmanager.repository.VerificationTokenRepository;
import com.vicente.taskmanager.scheduler.util.VerificationTokenSchedulerHelper;
import com.vicente.taskmanager.service.VerificationTokenSchedulerService;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class VerificationTokenSchedulerServiceImpl implements VerificationTokenSchedulerService {
    private final VerificationTokenRepository verificationTokenRepository;
    private final VerificationTokenSchedulerHelper verificationTokenSchedulerHelper;
    private static final Logger logger = LoggerFactory.getLogger(VerificationTokenSchedulerServiceImpl.class);

    public VerificationTokenSchedulerServiceImpl(
            VerificationTokenRepository verificationTokenRepository,
            VerificationTokenSchedulerHelper verificationTokenSchedulerHelper
    ) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.verificationTokenSchedulerHelper = verificationTokenSchedulerHelper;
    }

    @Override
    @Transactional(readOnly = true)
    public void deleteVerificationTokenTypeEmailExpiredBefore2Days() {
        OffsetDateTime thresholdDate = OffsetDateTime.now().minusDays(2);
        List<VerificationToken> verificationTokens = verificationTokenRepository.findByTypeAndExpiresAtBefore(
                TokenType.EMAIL_VERIFICATION, thresholdDate);

        deleteVerificationTokens(verificationTokens, thresholdDate);
    }

    @Override
    @Transactional(readOnly = true)
    public void deleteVerificationTokenTypePasswordExpiredBefore1Days() {
        OffsetDateTime thresholdDate = OffsetDateTime.now().minusDays(1);
        List<VerificationToken> verificationTokens = verificationTokenRepository.findByTypeAndExpiresAtBefore(
                TokenType.PASSWORD_RESET, thresholdDate);

        deleteVerificationTokens(verificationTokens, thresholdDate);
    }

    private void deleteVerificationTokens(List<VerificationToken> verificationTokens, OffsetDateTime thresholdDate) {
        if (!verificationTokens.isEmpty()) {
            AtomicInteger count = new AtomicInteger();
            verificationTokens.forEach(verificationToken -> {
                try {
                    verificationTokenSchedulerHelper.deleteSingleVerificationToken(verificationToken);
                    count.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                    logger.warn("[VERIFICATION TOKEN SCHEDULER] Verification token skipped due to optimistic lock" +
                            " | verificationTokenId={}", verificationToken.getId());
                }
            });
            if(count.get() > 0) {
                logger.info("[VERIFICATION TOKEN SCHEDULER] Verification tokens deleted | threshold={} count={}",
                        thresholdDate, count.get());
                return;
            }
            logger.warn("[VERIFICATION TOKEN SCHEDULER] Verification tokens found but none deleted due to concurrency");
        }else{
            logger.debug("[VERIFICATION TOKEN SCHEDULER] No Verification tokens to delete");
        }
    }
}
