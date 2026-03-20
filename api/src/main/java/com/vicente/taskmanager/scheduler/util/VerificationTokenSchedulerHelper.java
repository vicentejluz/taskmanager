package com.vicente.taskmanager.scheduler.util;

import com.vicente.taskmanager.domain.entity.VerificationToken;
import com.vicente.taskmanager.repository.VerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class VerificationTokenSchedulerHelper {
    private final VerificationTokenRepository verificationTokenRepository;
    private static final Logger logger = LoggerFactory.getLogger(VerificationTokenSchedulerHelper.class);

    public VerificationTokenSchedulerHelper(VerificationTokenRepository verificationTokenRepository) {
        this.verificationTokenRepository = verificationTokenRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteSingleVerificationToken(VerificationToken verificationToken) {
        logger.debug("[VERIFICATION TOKEN SCHEDULER] Executing Delete single verificationToken | verificationTokenId={}",
                verificationToken.getId());
        verificationTokenRepository.delete(verificationToken);
    }
}
