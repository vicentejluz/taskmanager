package com.vicente.taskmanager.service.impl;

import com.vicente.storage.StorageService;
import com.vicente.storage.security.LocalStorageHmacTokenGenerator;
import com.vicente.taskmanager.domain.entity.FileMetadata;
import com.vicente.taskmanager.dto.internal.FileDownloadResult;
import com.vicente.taskmanager.security.util.CryptoHelper;
import com.vicente.taskmanager.service.FileMetadataService;
import com.vicente.taskmanager.service.LocalSignedFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalSignedFileServiceImpl implements LocalSignedFileService {
    private final FileMetadataService fileMetadataService;
    private final StorageService storageService;
    private final String secret;
    private static final Logger logger = LoggerFactory.getLogger(LocalSignedFileServiceImpl.class);

    public LocalSignedFileServiceImpl(FileMetadataService fileMetadataService, StorageService storageService,
                                      @Value("${app.storage.local.secret-key}") String secret) {
        this.fileMetadataService = fileMetadataService;
        this.storageService = storageService;
        this.secret = secret;
    }

    @Override
    public void validateSignedFile(String token, String storageFileName, long expireAt, String contentDisposition) {
        validateExpiration(storageFileName, expireAt);

        String data = buildData(storageFileName, expireAt, contentDisposition);

        validateHmacToken(token, data, storageFileName);
    }

    @Override
    public FileDownloadResult downloadSigned(String storageFileName) {
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

    private static void validateExpiration(String storageFileName, long expireAt) {
        Instant now = Instant.now();
        Instant expireInstant = Instant.ofEpochSecond(expireAt);

        if (!now.isBefore(expireInstant)) {
            logger.debug("Signed URL expired | now={} | expireAt={}", now, expireInstant);

            String reason = String.format("Signed URL expired for file '%s'. Expiration time: %s",
                    storageFileName, expireInstant.atOffset(ZoneOffset.UTC));

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, reason);
        }

        logger.info("Signed URL expiration valid | now={} | expireAt={}", now, expireInstant);
    }

    private void validateHmacToken(String token, String data, String storageFileName) {
        String expectedToken;
        try {
            // Gera o HMAC esperado para os dados fornecidos
            expectedToken = LocalStorageHmacTokenGenerator.generateHmac256(secret, data);
        }catch (Exception e) {
            logger.error("Error generating HMAC token", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error generating validation token");
        }

        // Compara tokens de forma segura em tempo constante
        if(!CryptoHelper.safeEquals(expectedToken, token)) {
            logger.debug("Invalid token provided for signed URL | storageFileName={}", storageFileName);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid token for signed URL");
        }

        logger.info("Token validated successfully");
    }

    private static String buildData(String storageFileName, long expireAt, String contentDisposition) {
        String encodedContentDisposition = URLEncoder.encode(contentDisposition, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return storageFileName + ":" + expireAt + ":" + encodedContentDisposition;
    }
}
