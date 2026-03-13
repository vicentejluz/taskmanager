package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.domain.entity.RefreshToken;
import com.vicente.taskmanager.domain.entity.User;
import com.vicente.taskmanager.exception.RefreshTokenException;
import com.vicente.taskmanager.repository.RefreshTokenRepository;
import com.vicente.taskmanager.service.EmailService;
import com.vicente.taskmanager.service.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshExpiration;
    private final EmailService emailService;
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);

    public RefreshTokenServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${security.refresh.token.expiration.days}") long refreshExpiration,
            EmailService emailService
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpiration = refreshExpiration;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public String create(User user, String oldRefreshToken) {
        logger.info("Creating refresh token (login flow) | userId={}", user.getId());

        revokeOldRefreshTokenIfOwnedByUser(user.getId(), oldRefreshToken);
        return createNewToken(user);
    }

    @Override
    @Transactional
    public String create(User user, RefreshToken oldRefreshToken) {
        logger.info("Creating refresh token (rotation flow) | userId={}", user.getId());

        if (oldRefreshToken != null) {
            logger.debug("Revoking previous refresh token | tokenId={} | userId={}",
                    oldRefreshToken.getId(), user.getId());
            oldRefreshToken.setRevoked(true);
        }

        return createNewToken(user);
    }

    @Override
    public void validate(RefreshToken refreshToken) {
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

        if (refreshToken.isRevoked())
            return;

        refreshToken.setRevoked(true);

        refreshTokenRepository.save(refreshToken);
        logger.info("Refresh token revoked successfully. | tokenPrefix={}", tokenPrefix(token));
    }

    @Override
    @Transactional
    public void revokeAllTokens(Long userId) {
        logger.info("Revoking all refresh tokens | userId={}", userId);
        List<RefreshToken> refreshTokens = refreshTokenRepository.findByUser_IdAndRevokedFalse(userId);

        if (refreshTokens.isEmpty())
            return;

        refreshTokens.forEach(refreshToken -> refreshToken.setRevoked(true));

        refreshTokenRepository.saveAll(refreshTokens);
        logger.info("All refresh tokens revoked successfully | userId={} | count={}", userId, refreshTokens.size());
    }

    @Override
    @Transactional
    public void revokeAllTokensExceptCurrentToken(Long userId, String currentRefreshToken) {
        logger.info("Revoking all refresh tokens current refresh token | userId={}", userId);
        List<RefreshToken> refreshTokens = refreshTokenRepository.findByUser_IdAndRevokedFalse(userId);

        if (refreshTokens.isEmpty())
            return;

        refreshTokens.forEach(refreshToken -> {
            if (!refreshToken.getToken().equals(currentRefreshToken)) {
                refreshToken.setRevoked(true);
            }
        });

        refreshTokenRepository.saveAll(refreshTokens);
        logger.info("All refresh tokens revoked except current token successfully | userId={} | count={}",
                userId, refreshTokens.size());
    }

    @Override
    @Transactional
    public void handleReuseAttack(RefreshToken oldRefreshToken, String ipAddress) {
        if (oldRefreshToken.isReuseDetected()) {
            logger.debug("Reuse already handled | tokenId={}", oldRefreshToken.getId());
            return;
        }

        logger.warn("Refresh token reuse detected | id={}", oldRefreshToken.getId());

        User user = oldRefreshToken.getUser();

        revokeAllTokens(user.getId());

        user.incrementTokenVersion();

        refreshTokenRepository.markReuseDetected(oldRefreshToken.getId());

        emailService.sendSecurityAlert(user.getEmail(), ipAddress);
    }

    @Override
    @Transactional
    public RefreshToken findByTokenForUpdate(String token) {
        return refreshTokenRepository.findByTokenForUpdate(token).orElseThrow(() -> {
            logger.debug("Invalid refresh token");
            return new RefreshTokenException("Refresh token invalid!");
        });
    }

    private RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token).orElseThrow(() -> {
            logger.debug("Invalid refresh token | tokenPrefix={}", tokenPrefix(token));
            return new RefreshTokenException("Refresh token invalid!");
        });
    }

    private String tokenPrefix(String token) {
        return (token != null) ? token.substring(0, 8) : null;
    }

    private String createNewToken(User user) {
        String token = UUID.randomUUID().toString();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(refreshExpiration);
        RefreshToken refreshToken = new RefreshToken(token, expiresAt, user);

        refreshTokenRepository.saveAndFlush(refreshToken);
        logger.info("Refresh token created successfully | tokenId={} | userId={} | expiresAt={}",
                refreshToken.getId(), user.getId(), expiresAt);
        return token;
    }

    private void revokeOldRefreshTokenIfOwnedByUser(long userId, String oldRefreshToken) {
        if (oldRefreshToken == null)
            return;

        Optional<RefreshToken> optionalOldRefreshToken = refreshTokenRepository.findByToken(oldRefreshToken);

        optionalOldRefreshToken.ifPresent(refreshToken -> {
            if (refreshToken.getUser().getId().equals(userId)) {
                refreshToken.setRevoked(true);
                refreshTokenRepository.saveAndFlush(refreshToken);
                logger.debug("Previous refresh token revoked | tokenId={} | userId={}",
                        refreshToken.getId(), userId);
            }
        });
    }
}
