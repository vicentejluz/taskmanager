package com.vicente.storage;

import java.io.InputStream;

public interface StorageService {
    void upload(InputStream file, long contentLength, String objectKey, String mimeType);
    InputStream download(String objectKey);
    void delete(String objectKey);
}
