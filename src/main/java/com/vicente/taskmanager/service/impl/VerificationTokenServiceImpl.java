package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.exception.VerificationTokenException;
import com.vicente.taskmanager.model.entity.User;
import com.vicente.taskmanager.model.entity.VerificationToken;
import com.vicente.taskmanager.model.enums.AccountStatus;
import com.vicente.taskmanager.model.enums.TokenType;
import com.vicente.taskmanager.repository.VerificationTokenRepository;
import com.vicente.taskmanager.service.VerificationTokenService;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class VerificationTokenServiceImpl implements VerificationTokenService {
    private final VerificationTokenRepository verificationTokenRepository;
    private final EntityManager entityManager;
    private static final Logger logger = LoggerFactory.getLogger(VerificationTokenServiceImpl.class);
    
    
    public VerificationTokenServiceImpl(VerificationTokenRepository verificationTokenRepository,
                                        EntityManager entityManager) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void consumeToken(String token) {
        logger.info("Starting token verification");
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token).orElseThrow(
                () -> new VerificationTokenException("Invalid token"));

        validateTokenForConsumption(verificationToken);
        
        if(verificationToken.getType() == TokenType.EMAIL_VERIFICATION){
            verificationToken.getUser().setAccountStatus(AccountStatus.ACTIVE);
            if(verificationToken.getUser().getDeletedAt() != null) verificationToken.getUser().setDeletedAt(null);
        }

        markTokenAsConsumed(verificationToken);
        verificationTokenRepository.save(verificationToken);
        logger.info("Token verified successfully | tokenId={}", verificationToken.getId());
    }

    @Override
    @Transactional
    public VerificationToken generateOrReuseActiveToken(User user) {
        VerificationToken verificationToken;
        try {
            verificationToken = generateVerificationToken(user, TokenType.EMAIL_VERIFICATION);
        } catch (DataIntegrityViolationException ex) {
            logger.debug("Token already exists. Reusing active token | userId={}", user.getId());
            verificationToken = verificationTokenRepository.findByUser_IdAndTypeAndRevokedFalse(
                            user.getId(), TokenType.EMAIL_VERIFICATION)
                    .orElseThrow(() -> new IllegalStateException("Active token expected but not found"));
        }
        return verificationToken;
    }

    @Override
    @Transactional
    public Optional<VerificationToken> handleExistingActiveEmailVerificationToken(User user) {
        Optional<VerificationToken> optionalToken = verificationTokenRepository.findByUser_IdAndTypeAndRevokedFalse(
                                user.getId(), TokenType.EMAIL_VERIFICATION);

        if (optionalToken.isEmpty()) {
            return Optional.empty();
        }

        VerificationToken token = optionalToken.get();
        logger.debug("Existing verification token found | userId={} | used={} | expired={} | revoked={}",
                user.getId(), token.isUsed(), token.isExpired(), token.isRevoked());

        if (!token.isExpired() && !token.isUsed()) {
            logger.debug("Reusing existing valid verification token | userId={}", user.getId());
            return Optional.of(token);
        }

        logger.debug("Existing verification token is no longer valid. Marking as revoked | userId={}", user.getId());
        token.setRevoked(true);

        entityManager.flush();

        return Optional.empty();
    }

    private VerificationToken generateVerificationToken(User user, TokenType tokenType) {
        VerificationToken verificationToken = new VerificationToken(
                UUID.randomUUID().toString(), tokenType,
                OffsetDateTime.now().plusMinutes(tokenType.getExpirationMinutes()), user);

        return verificationTokenRepository.save(verificationToken);
    }

    private void markTokenAsConsumed(VerificationToken verificationToken) {
        verificationToken.setUsed(true);
        verificationToken.setRevoked(true);
    }

    private void validateTokenForConsumption(VerificationToken verificationToken) {
        if (verificationToken.isUsed() || verificationToken.isExpired() || verificationToken.isRevoked()) {
            logger.debug("Invalid token consumption attempt | tokenId={}", verificationToken.getId());
            throw new VerificationTokenException("Invalid or expired token");
        }
    }
}
