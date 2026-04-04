package com.vicente.storage;

import com.vicente.storage.dto.StorageObject;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public interface StorageService {
    void upload(Path file, long contentLength, String objectKey, String mimeType);
    InputStream download(String objectKey);
    String generateSignedUrl(String objectKey, Duration duration, String contentDisposition);
    void delete(String objectKey);
    boolean exists(String objectKey);
    List<StorageObject> list(String path);

    default String buildObjectKey(String path, String storedFileName) {
        return path.replaceAll("/+$", "") + "/" + storedFileName;
    }
}
