package com.vicente.taskmanager.security.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistService {
    private final TokenService tokenService;
    private final StringRedisTemplate stringRedisTemplate;
    private static final String ACCESS_BLACKLIST_KEY_PREFIX = "auth:jwt:blacklist:";

    public TokenBlacklistService(
            TokenService tokenService, StringRedisTemplate stringRedisTemplate
    ) {
        this.tokenService = tokenService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void blacklistToken(String token){
        if(token == null) return;
        String jti = tokenService.getClaims(token).getId();
        Date expirationDate = tokenService.getClaims(token).getExpiration();
        if(jti == null || expirationDate == null) return;
        storeBlacklist(jti, expirationDate);
    }

    private void storeBlacklist(String jti, Date expirationDate) {
        String accessBlacklistKey = buildKey(jti);
        long ttl = getTimeOut(expirationDate);

        if(ttl <= 0) return;

        stringRedisTemplate.opsForValue().set(accessBlacklistKey, "1", ttl, TimeUnit.SECONDS);
    }

    public boolean isBlacklisted(String jti) {
        if(jti == null) return false;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(buildKey(jti)));
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
