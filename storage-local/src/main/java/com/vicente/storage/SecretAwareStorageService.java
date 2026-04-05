package com.vicente.storage;

import com.vicente.storage.exception.SignedUrlValidationException;
import com.vicente.storage.exception.StorageException;

import java.time.Duration;

public interface SecretAwareStorageService extends StorageService {
    /**
     * Gera uma URL assinada para o arquivo.
     * @param objectKey name of the file in storage
     * @param duration validity duration of the URL
     * @param contentDisposition value for Content-Disposition
     * @param secret secret used to generate the HMAC
     * @return signed URL
     * @throws StorageException if the secret is missing or there are internal HMAC issues
     */
    String generateSignedUrl(String objectKey, Duration duration, String contentDisposition, String secret);

    /**
     * Valida uma URL assinada de um arquivo no LocalStorage.
     *
     * @param token signed URL token
     * @param storageFileName name of the file in storage
     * @param expireAt expiration timestamp
     * @param contentDisposition value for Content-Disposition
     * @param secret secret used to generate the HMAC
     * @throws SignedUrlValidationException if the token is invalid, expired, or the secret is missing
     */
    void validateSignedUrl(String token, String storageFileName, long expireAt, String contentDisposition, String secret);
}
