package com.vicente.taskmanager.service;

import com.vicente.taskmanager.dto.internal.FileDownloadResult;

public interface LocalSignedFileService {
    void validateSignedFile(String token, String storageFileName, long expireAt, String contentDisposition);
    FileDownloadResult downloadSigned(String storageFileName);
}