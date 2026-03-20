package com.vicente.taskmanager.security.service;

import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class AuthTokenStoreService {
    private final TokenService tokenService;
    private final StringRedisTemplate stringRedisTemplate;
    private static final String ACCESS_BLACKLIST_KEY_PREFIX = "auth:jwt:blacklist:";
    private static final Logger logger = LoggerFactory.getLogger(AuthTokenStoreService.class);

    public AuthTokenStoreService(TokenService tokenService, StringRedisTemplate stringRedisTemplate) {
        this.tokenService = tokenService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void blacklistToken(String token){
        logger.info("Starting access token blacklist process");
        if(token == null){
            logger.debug("Access token is null, skipping blacklist");
            return;
        }
        Claims claims = tokenService.getClaims(token);
        if(claims == null) {
            logger.debug("Missing token data for blacklist");
            return;
        }

        String jti = claims.getId();
        Date expirationDate = claims.getExpiration();

        logger.info("Blacklisting access token");
        storeBlacklist(jti, expirationDate);
        logger.info("Access token successfully processed for blacklist");
    }

    public boolean isBlacklisted(String jti) {
        if(jti == null) return false;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(buildKey(jti)));
    }

    private void storeBlacklist(String jti, Date expirationDate) {
        String accessBlacklistKey = buildKey(jti);
        long ttl = getTimeOut(expirationDate);

        logger.info("Storing access token in redis blacklist | jtiPrefix={} ttl={}", jti.substring(0, 8), ttl);

        if(ttl <= 0){
            logger.debug("Skipping blacklist storage due to invalid TTL | ttl={}", ttl);
            return;
        }

        stringRedisTemplate.opsForValue().set(accessBlacklistKey, "1", ttl, TimeUnit.SECONDS);

        logger.info("Access token successfully added to redis blacklist");
    }

    private long getTimeOut(Date expirationDate) {
        long now = Instant.now().getEpochSecond();
        long exp = expirationDate.toInstant().getEpochSecond();
        return exp - now;
    }

    private String buildKey(String jti){
        return ACCESS_BLACKLIST_KEY_PREFIX + jti;
    }
}
