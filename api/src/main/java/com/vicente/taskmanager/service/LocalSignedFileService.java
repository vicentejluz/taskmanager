package com.vicente.taskmanager.service;

import com.vicente.taskmanager.dto.internal.FileDownloadResult;

import java.time.Duration;

public interface LocalSignedFileService {
    String generateSignedUrl(String objectKey, Duration signedUrlExpiration, String contentDisposition);
    FileDownloadResult downloadSigned(String token, String storageFileName, long expireAt, String contentDisposition);
}