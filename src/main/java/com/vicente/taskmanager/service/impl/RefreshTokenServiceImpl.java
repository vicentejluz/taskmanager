package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.domain.entity.RefreshToken;
import com.vicente.taskmanager.domain.entity.User;
import com.vicente.taskmanager.exception.RefreshTokenException;
import com.vicente.taskmanager.repository.RefreshTokenRepository;
import com.vicente.taskmanager.service.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshExpiration;
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);

    public RefreshTokenServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${security.refresh.token.expiration.days}") long refreshExpiration
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpiration = refreshExpiration;
    }

    @Override
    @Transactional
    public String create(User user) {
        String token = UUID.randomUUID().toString();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(refreshExpiration);
        RefreshToken refreshToken = new RefreshToken(token, expiresAt, user);

        logger.info("Created refresh token | userId={}", user.getId());
        refreshTokenRepository.save(refreshToken);
        return token;
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken validate(String token) {
        logger.info("Validating refresh token | tokenPrefix={}", tokenPrefix(token));
        RefreshToken refreshToken = findByToken(token);

        if(refreshToken.isRevoked()){
            logger.debug("Refresh token has been revoked | tokenPrefix={}", tokenPrefix(token));
            throw new RefreshTokenException("Refresh token is revoked!");
        }

        if(refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())){
            logger.debug("Expired refresh token | tokenPrefix={}", tokenPrefix(token));
            throw new RefreshTokenException("Refresh token is expired!");
        }

        logger.info("Refresh token validated | tokenPrefix={}", tokenPrefix(token));
        return refreshToken;
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

        if(refreshToken.isRevoked()) return;

        refreshToken.setRevoked(true);

        refreshTokenRepository.save(refreshToken);
        logger.info("Refresh token revoked successfully. | tokenPrefix={}", tokenPrefix(token));
    }

    private RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token).orElseThrow(() -> {
            logger.debug("Invalid refresh token");
            return new RefreshTokenException("Refresh token invalid!");
        });
    }

    private String tokenPrefix(String token) {
        return (token != null) ? token.substring(0, 8) : null;
    }
}
