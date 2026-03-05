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
            @Value("${security.refresh.token.expiration.minutes}") long refreshExpiration
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpiration = refreshExpiration;
    }

    @Override
    @Transactional
    public String create(User user) {
        String token = UUID.randomUUID().toString();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(refreshExpiration);
        RefreshToken refreshToken = new RefreshToken(token, expiresAt, user);

        logger.info("Created refresh token | userId={}", user.getId());
        refreshTokenRepository.save(refreshToken);
        return token;
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken validate(String token) {
        logger.info("Validating refresh token | tokenPrefix={}", token.substring(0,8));
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token).orElseThrow(() -> {
            logger.debug("Invalid refresh token | token={}", token.substring(0,8));
            return new RefreshTokenException("Refresh token invalid!");
        });

        if(refreshToken.isRevoked()){
            logger.debug("Refresh token has been revoked | tokenPrefix={}", token.substring(0,8));
            throw new RefreshTokenException("Refresh token is revoked!");
        }

        if(refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())){
            logger.debug("Expired refresh token | tokenPrefix={}", token.substring(0,8));
            throw new RefreshTokenException("Refresh token is expired!");
        }

        logger.info("Refresh token validated | tokenPrefix={}", token.substring(0,8));
        return refreshToken;
    }
}
