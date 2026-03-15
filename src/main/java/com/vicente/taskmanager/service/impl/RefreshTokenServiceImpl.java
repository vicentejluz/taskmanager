package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.domain.entity.RefreshToken;
import com.vicente.taskmanager.domain.entity.User;
import com.vicente.taskmanager.dto.internal.RefreshTokenResult;
import com.vicente.taskmanager.exception.RefreshTokenException;
import com.vicente.taskmanager.repository.RefreshTokenRepository;
import com.vicente.taskmanager.security.util.CryptoHelper;
import com.vicente.taskmanager.service.EmailService;
import com.vicente.taskmanager.service.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshExpiration;
    private final long refreshTokenGraceWindow;
    private final EmailService emailService;
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);

    public RefreshTokenServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${security.refresh.token.expiration.days}") long refreshExpiration,
            @Value("${security.refresh.token.grace.window.seconds}") long refreshTokenGraceWindow,
            EmailService emailService
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpiration = refreshExpiration;
        this.refreshTokenGraceWindow = refreshTokenGraceWindow;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public RefreshTokenResult create(User user, String oldRefreshToken) {
        logger.info("Creating refresh token (login flow) | userId={}", user.getId());

        revokeOldRefreshTokenIfOwnedByUser(user.getId(), oldRefreshToken);

        return createNewToken(user);
    }

    @Override
    @Transactional
    public String create(User user, RefreshToken oldRefreshToken, String oldFingerprint) {
        logger.info("Creating refresh token (rotation flow) | userId={}", user.getId());

        if (oldRefreshToken != null) {
            logger.debug("Revoking previous refresh token | tokenId={} | userId={}",
                    oldRefreshToken.getId(), user.getId());
            oldRefreshToken.setRevokedAt(OffsetDateTime.now());
        }

        return createNewToken(user, Objects.requireNonNull(oldRefreshToken).getTokenFamilyId(),
                oldFingerprint);
    }

    @Override
    public void validateExpiration(RefreshToken refreshToken) {
        logger.info("Validating refresh token | tokenPrefix={}", tokenPrefix(refreshToken.getToken()));

        if (refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            logger.debug("Expired refresh token | tokenPrefix={}", tokenPrefix(refreshToken.getToken()));
            throw new RefreshTokenException("Refresh token is expired!");
        }

        logger.info("Refresh token validated | tokenPrefix={}", tokenPrefix(refreshToken.getToken()));
    }

    @Override
    @Transactional
    public void revokeToken(String token, Long userId) {
        logger.info("Revoking Refresh token | tokenPrefix={}", tokenPrefix(token));
        RefreshToken refreshToken = findByToken(token);

        if (!refreshToken.getUser().getId().equals(userId)) {
            logger.debug("Refresh token does not belong to the authenticated user | tokenUserId={} | requestUserId={}",
                    refreshToken.getUser().getId(), userId);
            throw new RefreshTokenException("Refresh token does not belong to the authenticated user.");
        }

        if (refreshToken.getRevokedAt() != null)
            return;

        refreshToken.setRevokedAt(OffsetDateTime.now());

        refreshTokenRepository.save(refreshToken);
        logger.info("Refresh token revoked successfully. | tokenPrefix={}", tokenPrefix(token));
    }

    @Override
    @Transactional
    public void revokeAllTokens(Long userId) {
        logger.info("Revoking all refresh tokens | userId={}", userId);
        List<RefreshToken> refreshTokens = refreshTokenRepository.findByUser_IdAndRevokedAtNull(userId);

        if (refreshTokens.isEmpty())
            return;

        refreshTokens.forEach(refreshToken -> refreshToken.setRevokedAt(OffsetDateTime.now()));

        refreshTokenRepository.saveAll(refreshTokens);
        logger.info("All refresh tokens revoked successfully | userId={} | count={}", userId, refreshTokens.size());
    }

    @Override
    @Transactional
    public void revokeAllTokensExceptCurrentToken(Long userId, String currentRefreshToken) {
        logger.info("Revoking all refresh tokens current refresh token | userId={}", userId);
        List<RefreshToken> refreshTokens = refreshTokenRepository.findByUser_IdAndRevokedAtNull(userId);

        if (refreshTokens.isEmpty())
            return;

        String hashed = CryptoHelper.hashValue(currentRefreshToken);

        refreshTokens.forEach(refreshToken -> {
            if (!CryptoHelper.safeEquals(refreshToken.getToken(),
                    hashed)) {
                refreshToken.setRevokedAt(OffsetDateTime.now());
            }
        });

        refreshTokenRepository.saveAll(refreshTokens);
        logger.info("All refresh tokens revoked except current token successfully | userId={} | count={}",
                userId, refreshTokens.size());
    }

    @Override
    @Transactional
    public void handleTokenCompromise(RefreshToken oldRefreshToken, String ipAddress) {
        if (oldRefreshToken.isReuseDetected()) {
            logger.debug("Reuse already handled | tokenId={}", oldRefreshToken.getId());
            return;
        }

        OffsetDateTime threshold =
                OffsetDateTime.now().minusSeconds(refreshTokenGraceWindow);

        if(oldRefreshToken.getRevokedAt() == null || oldRefreshToken.getRevokedAt().isBefore(threshold)) {
            logger.warn("Refresh token reuse detected | id={}", oldRefreshToken.getId());

            User user = oldRefreshToken.getUser();

            revokeFamilyTokens(user.getId(), oldRefreshToken.getTokenFamilyId());

            user.incrementTokenVersion();

            refreshTokenRepository.markReuseDetected(oldRefreshToken.getId());

            emailService.sendSecurityAlert(user.getEmail(), ipAddress);
        }
    }

    @Override
    @Transactional
    public RefreshToken findByTokenForUpdate(String token) {
        return refreshTokenRepository.findByTokenForUpdate(CryptoHelper.hashValue(token)).orElseThrow(
                () -> {
                    logger.debug("Invalid refresh token");
                    return new RefreshTokenException("Refresh token invalid!");
                });
    }

    @Override
    public boolean matchesFingerprint(RefreshToken oldRefreshToken, String fingerprint){
        return CryptoHelper.safeEquals(
                oldRefreshToken.getFingerprint(), CryptoHelper.hashValue(fingerprint));
    }

    private RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(CryptoHelper.hashValue(token)).orElseThrow(
                () -> {
                    logger.debug("Invalid refresh token | tokenPrefix={}", tokenPrefix(token));
                    return new RefreshTokenException("Refresh token invalid!");
                });
    }

    private String tokenPrefix(String token) {
        return (token != null) ? token.substring(0, 8) : null;
    }

    private RefreshTokenResult createNewToken(User user) {
        UUID tokenFamilyId = UUID.randomUUID();
        String fingerprint = CryptoHelper.generateSecureRandomValue();

        String token = createNewRefreshToken(user, tokenFamilyId, fingerprint);

        return new RefreshTokenResult(token, fingerprint);
    }

    private String createNewToken(User user, UUID tokenFamilyId, String fingerprint) {
        return createNewRefreshToken(user, tokenFamilyId, fingerprint);
    }

    private String createNewRefreshToken(User user, UUID tokenFamilyId, String fingerprint) {
        String token = CryptoHelper.generateSecureRandomValue();

        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(refreshExpiration);
        RefreshToken refreshToken = new RefreshToken(CryptoHelper.hashValue(token), tokenFamilyId,
                CryptoHelper.hashValue(fingerprint), expiresAt, user);

        refreshTokenRepository.saveAndFlush(refreshToken);
        logger.info("Refresh token created successfully | tokenId={} | userId={} | expiresAt={}",
                refreshToken.getId(), user.getId(), expiresAt);
        return token;
    }

    private void revokeOldRefreshTokenIfOwnedByUser(long userId, String oldRefreshToken) {
        if (oldRefreshToken == null)
            return;

        Optional<RefreshToken> optionalOldRefreshToken = refreshTokenRepository.findByToken(
                CryptoHelper.hashValue(oldRefreshToken));

        optionalOldRefreshToken.ifPresent(refreshToken -> {
            if (refreshToken.getUser().getId().equals(userId)) {
                refreshToken.setRevokedAt(OffsetDateTime.now());
                refreshTokenRepository.saveAndFlush(refreshToken);
                logger.debug("Previous refresh token revoked | tokenId={} | userId={}",
                        refreshToken.getId(), userId);
            }
        });
    }

    private void revokeFamilyTokens(Long userId, UUID tokenFamilyId) {
        logger.info("Revoking family tokens | userId={} tokenFamilyId={}", userId, tokenFamilyId);
        List<RefreshToken> refreshTokens = refreshTokenRepository.findByUser_IdAndTokenFamilyId(userId, tokenFamilyId);

        if (refreshTokens.isEmpty())
            return;

        OffsetDateTime now = OffsetDateTime.now();

        refreshTokens.forEach(refreshToken -> refreshToken.setRevokedAt(now));

        refreshTokenRepository.saveAll(refreshTokens);
        logger.info("All family tokens revoked successfully | userId={} tokenFamilyId={} count={}",
                userId, refreshTokens.size(), tokenFamilyId);
    }
}
