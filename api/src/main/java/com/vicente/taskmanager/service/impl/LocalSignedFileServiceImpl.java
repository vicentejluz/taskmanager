package com.vicente.taskmanager.service.impl;

import com.vicente.storage.SecretAwareStorageService;
import com.vicente.storage.StorageService;
import com.vicente.storage.exception.StorageException;
import com.vicente.taskmanager.domain.entity.FileMetadata;
import com.vicente.taskmanager.dto.internal.FileDownloadResult;
import com.vicente.taskmanager.service.FileMetadataService;
import com.vicente.taskmanager.service.LocalSignedFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalSignedFileServiceImpl implements LocalSignedFileService {
    private final FileMetadataService fileMetadataService;
    private final StorageService storageService;
    private final String baseUrl;
    private final String secret;
    private static final Logger logger = LoggerFactory.getLogger(LocalSignedFileServiceImpl.class);

    public LocalSignedFileServiceImpl(FileMetadataService fileMetadataService, StorageService storageService,
                                      @Value("${app.storage.local.base-url:http://localhost:8080/api/v1/}") String baseUrl,
                                      @Value("${app.storage.local.secret-key}") String secret) {
        this.fileMetadataService = fileMetadataService;
        this.storageService = storageService;
        this.baseUrl = baseUrl;
        this.secret = secret;
    }

    @Override
    public String generateSignedUrl(String objectKey, Duration signedUrlExpiration, String contentDisposition) {
        if(!(storageService instanceof SecretAwareStorageService secretAwareStorageService)) {
            logger.debug("Current storage does not support signed URL generation | storageClass={}",
                    storageService.getClass().getSimpleName());

            throw new StorageException(
                    "Signed URL generation is not supported for the current storage configuration.",
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        String signedUrl =  secretAwareStorageService.generateSignedUrl(
                objectKey, signedUrlExpiration, contentDisposition, secret);
        return buildLocalSignedUrl(signedUrl);
    }

    @Override
    public FileDownloadResult downloadSigned(String token, String storageFileName, long expireAt, String contentDisposition) {
        validateSignedFile(token, storageFileName, expireAt, contentDisposition);

        logger.debug("Download signed file requested | storageFileName={}", storageFileName);

        FileMetadata fileMetadata = fileMetadataService.findByStoredFileName(storageFileName);

        String objectKey = storageService.buildObjectKey(fileMetadata.getPath(), fileMetadata.getStoredFileName());

        logger.debug("Resolved object key for download | storageFileName={} objectKey={}",
                storageFileName, objectKey);

        InputStream inputStream = storageService.download(objectKey);

        logger.info("Signed file download started | storageFileName={} fileName={} size={} contentType={}",
                storageFileName, fileMetadata.getFileName(), fileMetadata.getSize(), fileMetadata.getContentType());

        return new FileDownloadResult(fileMetadata.getFileName(), inputStream, fileMetadata.getSize(),
                fileMetadata.getContentType());
    }

    private String buildLocalSignedUrl(String signedUrl) {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        String storagePath = "files/taskmanager-files/";
        String relativeFilePathWithQuery = (signedUrl.startsWith("/")) ? signedUrl.substring(1) : signedUrl;

        URI uri = URI.create(normalizedBaseUrl).resolve(storagePath + relativeFilePathWithQuery).normalize();

        return uri.toString();
    }

    private void validateSignedFile(String token, String storageFileName, long expireAt, String contentDisposition) {
        if(!(storageService instanceof SecretAwareStorageService secretAwareStorageService)) {
            logger.debug("Signed URL validation not supported by current storage | storageClass={}",
                    storageService.getClass().getSimpleName());

            throw new StorageException("Signed URL validation is not supported for the current storage configuration.",
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        logger.debug("Validating signed file | storageFileName={} | expireAt={}", storageFileName, expireAt);

        secretAwareStorageService.validateSignedUrl(token, storageFileName, expireAt, contentDisposition, secret);
    }
}
