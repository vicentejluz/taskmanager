package com.vicente.storage;

import java.io.InputStream;

public class AzureBlobStorageService implements StorageService {
    @Override
    public void upload(InputStream file, long contentLength, String objectKey, String mimeType) {

    }

    @Override
    public InputStream download(String objectKey) {
        return null;
    }

    @Override
    public void delete(String objectKey) {

    }
}
