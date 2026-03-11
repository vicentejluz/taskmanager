package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.domain.entity.RefreshToken;
import com.vicente.taskmanager.repository.RefreshTokenRepository;
import com.vicente.taskmanager.scheduler.util.RefreshTokenSchedulerHelper;
import com.vicente.taskmanager.service.RefreshTokenSchedulerService;
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
public class RefreshTokenSchedulerServiceImpl implements RefreshTokenSchedulerService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenSchedulerHelper refreshTokenSchedulerHelper;
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenSchedulerServiceImpl.class);

    public RefreshTokenSchedulerServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenSchedulerHelper refreshTokenSchedulerHelper
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenSchedulerHelper = refreshTokenSchedulerHelper;
    }

    @Override
    @Transactional(readOnly = true)
    public void deleteRefreshTokensExpiredBefore7Days() {
        OffsetDateTime thresholdDate = OffsetDateTime.now().minusDays(7);
        List<RefreshToken> refreshTokens = refreshTokenRepository.findByExpiresAtBefore(thresholdDate);

        deleteRefreshTokens(refreshTokens,  thresholdDate);
    }

    @Override
    @Transactional(readOnly = true)
    public void deleteRefreshTokensOfUsersDeletedBefore3Days() {
        OffsetDateTime thresholdDate = OffsetDateTime.now().minusDays(3);
        List<RefreshToken> refreshTokens = refreshTokenRepository.findByUser_DeletedAtBefore(thresholdDate);

        deleteRefreshTokens(refreshTokens,  thresholdDate);
    }

    private void deleteRefreshTokens(List<RefreshToken> refreshTokens, OffsetDateTime thresholdDate) {
        if (!refreshTokens.isEmpty()) {
            AtomicInteger count = new AtomicInteger();
            refreshTokens.forEach(refreshToken -> {
                try {
                    refreshTokenSchedulerHelper.deleteSingleRefreshToken(refreshToken);
                    count.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                    logger.warn("[REFRESH TOKEN SCHEDULER] Refresh token skipped due to optimistic lock" +
                            " | refreshTokenId={}", refreshToken.getId());
                }
            });
            if(count.get() > 0) {
                logger.info("[REFRESH TOKEN SCHEDULER] Refresh tokens deleted | threshold={} count={}", thresholdDate, count.get());
                return;
            }
            logger.warn("[REFRESH TOKEN SCHEDULER] Refresh tokens found but none deleted due to concurrency");
        }else{
            logger.debug("[REFRESH TOKEN SCHEDULER] No refresh tokens to delete");
        }
    }
}
