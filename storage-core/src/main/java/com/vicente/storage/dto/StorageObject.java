package com.vicente.storage.dto;

import java.time.OffsetDateTime;

public record StorageObject(
        String objectKey,
        Long contentLength,
        OffsetDateTime lastModified
) {
}
