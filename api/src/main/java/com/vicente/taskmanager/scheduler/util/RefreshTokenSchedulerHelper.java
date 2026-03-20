package com.vicente.taskmanager.scheduler.util;

import com.vicente.taskmanager.domain.entity.RefreshToken;
import com.vicente.taskmanager.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RefreshTokenSchedulerHelper {
    private final RefreshTokenRepository refreshTokenRepository;
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenSchedulerHelper.class);

    public RefreshTokenSchedulerHelper(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteSingleRefreshToken(RefreshToken refreshToken) {
        logger.debug("[REFRESH TOKEN SCHEDULER] Executing Delete single refreshToken | refreshTokenId={}", refreshToken.getId());
        refreshTokenRepository.delete(refreshToken);
    }
}
