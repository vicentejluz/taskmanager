package com.vicente.taskmanager.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Component
public class FileShareUrlCache {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration signedUrlExpiration;
    private static final String FILE_SHARE_URL_KEY_PREFIX = "file:share:url:";
    private static final long TTL_BUFFER_MINUTES = 3L;
    private static final Logger logger = LoggerFactory.getLogger(FileShareUrlCache.class);

    public FileShareUrlCache(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper,
                             @Value("${app.storage.signed-url-expiration}") Duration signedUrlExpiration) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.signedUrlExpiration = signedUrlExpiration;
    }

    public FileShareCacheEntry getShareUrl(long fileId) {
        logger.info("Retrieving share URL from cache | fileId={}", fileId);

        Object value = redisTemplate.opsForValue().get(buildKey(fileId));

        if(value == null) return null;

        // O GenericJacksonJsonRedisSerializer retorna um objeto genérico (Map),
        // então usamos o ObjectMapper para convertê-lo para FileShareCacheEntry
        return objectMapper.convertValue(value, FileShareCacheEntry.class);
    }

    public void storeShareUrl(long fileId, FileShareCacheEntry entry) {
        if(entry.url() == null || entry.url().isBlank()) {
            logger.debug("Skipping cache storage due to missing URL | fileId={}", fileId);
            return;
        }

        if(entry.expireAt() == null) {
            logger.debug("Skipping cache storage due to missing expireAt | fileId={}", fileId);
            return;
        }

        Duration ttl = signedUrlExpiration.minusMinutes(TTL_BUFFER_MINUTES);

        if(ttl.isZero() || ttl.isNegative()) {
            logger.debug("Skipping cache storage due to non-positive TTL | fileId={} ttl={}s",
                    fileId, ttl.getSeconds());
            return;
        }

        logger.info("Storing share URL in cache | fileId={} ttl={}s",
                fileId, ttl.getSeconds());


        redisTemplate.opsForValue().set(
                buildKey(fileId), entry, ttl);
    }

    public void removeShareUrl(long fileId) {
        boolean removed = redisTemplate.delete(buildKey(fileId));
        logger.info("Removing share URL from cache | fileId={} | removed={}", fileId, removed);
    }

    private String buildKey(long fileId){
        return FILE_SHARE_URL_KEY_PREFIX + fileId;
    }
}
