package com.vicente.taskmanager.cache;

import java.time.OffsetDateTime;

public record FileShareCacheEntry(
        String url,
        OffsetDateTime expireAt
) {
}
