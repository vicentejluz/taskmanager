package com.vicente.storage;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.vicente.storage.exception.StorageException;

import java.io.*;

public class AzureBlobStorageService implements StorageService {
    private final BlobContainerClient blobContainerClient;

    public AzureBlobStorageService(BlobContainerClient blobContainerClient) {
        this.blobContainerClient = blobContainerClient;
    }

    @Override
    public void upload(InputStream file, long contentLength, String objectKey, String mimeType) {
        try {
            BlobClient blobClient = blobContainerClient.getBlobClient(objectKey);
            blobClient.uploadWithResponse( new BlobParallelUploadOptions(BinaryData.fromStream(
                            file, contentLength)).setHeaders(new BlobHttpHeaders().setContentType(mimeType)),
                    null, null);
        }catch (HttpResponseException e) {
            throw new StorageException("Error sending file to Azure blob: " + e.getMessage(), e.getResponse().getStatusCode(), e);
        }
    }

    @Override
    public InputStream download(String objectKey) {
        try{
            BlobClient blobClient = blobContainerClient.getBlobClient(objectKey);

            return blobClient.openInputStream();
        }catch (HttpResponseException e) {
        throw new StorageException("Error download file from Azure blob: " + e.getMessage(), e.getResponse().getStatusCode(), e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try{
            BlobClient blobClient = blobContainerClient.getBlobClient(objectKey);
            blobClient.delete();
        }catch (HttpResponseException e) {
            throw new StorageException("Error deleting file from Azure blob: " + e.getMessage(), e.getResponse().getStatusCode(), e);
        }

    }
}
