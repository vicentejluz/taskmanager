package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.exception.VerificationTokenException;
import com.vicente.taskmanager.model.entity.User;
import com.vicente.taskmanager.model.entity.VerificationToken;
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
    public void consumeToken(VerificationToken verificationToken) {
        logger.debug("Consuming token | tokenId={} | userId={}",
                verificationToken.getId(), verificationToken.getUser().getId());

        markTokenAsConsumed(verificationToken);
        verificationTokenRepository.save(verificationToken);

        logger.info("Token consumed successfully | userId={} | tokenId={}",
                verificationToken.getUser().getId(), verificationToken.getId());
    }

    @Override
    @Transactional
    public VerificationToken generateOrReuseActiveToken(User user, TokenType tokenType) {
        VerificationToken verificationToken;
        try {
            verificationToken = generateVerificationToken(user, tokenType);
        } catch (DataIntegrityViolationException ex) {
            logger.debug("Token already exists. Reusing active token | userId={}", user.getId());
            verificationToken = verificationTokenRepository.findByUser_IdAndTypeAndRevokedFalse(
                            user.getId(), tokenType)
                    .orElseThrow(() -> new IllegalStateException("Active token expected but not found"));
        }
        return verificationToken;
    }

    @Override
    @Transactional
    public VerificationToken getOrCreateActiveVerificationToken(User user, TokenType tokenType) {
        Optional<VerificationToken> optionalToken = verificationTokenRepository.findByUser_IdAndTypeAndRevokedFalse(
                                user.getId(), tokenType);

        if (optionalToken.isEmpty()) {
            return generateOrReuseActiveToken(user, tokenType);
        }

        VerificationToken verificationToken = optionalToken.get();
        logger.debug("Existing verification token found | userId={} | used={} | expired={} | revoked={}",
                user.getId(), verificationToken.isUsed(), verificationToken.isExpired(), verificationToken.isRevoked());

        if (!verificationToken.isExpired() && !verificationToken.isUsed()) {
            logger.debug("Reusing existing valid verification token | userId={}", user.getId());
            return verificationToken;
        }

        logger.debug("Existing verification token is no longer valid. Marking as revoked | userId={}", user.getId());
        verificationToken.setRevoked(true);

        entityManager.flush();

        return generateOrReuseActiveToken(user, tokenType);
    }

    @Override
    @Transactional(readOnly = true)
    public VerificationToken findByToken(String token) {
        return verificationTokenRepository.findByToken(token).orElseThrow(() -> {
                    logger.debug("Email confirmation failed: token not found");
                    return new VerificationTokenException("Invalid token");
                });
    }

    @Override
    public void validateTokenForConsumption(VerificationToken verificationToken) {
        if (verificationToken.isUsed() || verificationToken.isExpired() || verificationToken.isRevoked()) {
            logger.debug("Invalid token consumption attempt | tokenId={}", verificationToken.getId());
            throw new VerificationTokenException("Invalid or expired token");
        }
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
}
