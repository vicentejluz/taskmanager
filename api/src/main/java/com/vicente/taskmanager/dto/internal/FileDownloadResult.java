package com.vicente.taskmanager.dto.internal;

import java.io.InputStream;

public record FileDownloadResult(
        String filename,
        InputStream inputStream,
        Long size,
        String contentType
) {
}
