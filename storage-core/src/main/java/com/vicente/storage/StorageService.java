package com.vicente.storage;

import com.vicente.storage.dto.StorageObject;

import java.io.InputStream;
import java.util.List;

public interface StorageService {
    void upload(InputStream file, long contentLength, String objectKey, String mimeType);
    InputStream download(String objectKey);
    void delete(String objectKey);
    boolean exists(String objectKey);
    List<StorageObject> list(String path);
}
