package com.vicente.storage;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.vicente.storage.exception.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class AzureBlobStorageService implements StorageService {
    private final BlobContainerClient blobContainerClient;
    private static final Logger logger = LoggerFactory.getLogger(AzureBlobStorageService.class);

    public AzureBlobStorageService(BlobContainerClient blobContainerClient) {
        this.blobContainerClient = blobContainerClient;
        logger.info("AzureBlobStorageService initialized for container: {}", blobContainerClient.getBlobContainerName());
    }

    @Override
    public void upload(InputStream file, long contentLength, String objectKey, String mimeType) {
        try {
            BlobClient blobClient = blobContainerClient.getBlobClient(objectKey);

            logger.info("Uploading file '{}' to Azure Blob ({} bytes, MIME: {})", objectKey, contentLength, mimeType);

            blobClient.uploadWithResponse( new BlobParallelUploadOptions(BinaryData.fromStream(
                            file, contentLength)).setHeaders(new BlobHttpHeaders().setContentType(mimeType)),
                    null, null);

            logger.info("Successfully uploaded file '{}'", objectKey);
        }catch (HttpResponseException e) {
            logger.error("Failed to upload file '{}' to Azure Blob", objectKey, e);
            throw new StorageException("Error sending file to Azure blob: " + e.getMessage(), e.getResponse().getStatusCode(), e);
        }
    }

    @Override
    public InputStream download(String objectKey) {
        try{
            BlobClient blobClient = blobContainerClient.getBlobClient(objectKey);

            logger.info("Downloading file '{}' from Azure Blob", objectKey);

            return blobClient.openInputStream();
        }catch (HttpResponseException e) {
            logger.error("Failed to download file '{}' from Azure Blob", objectKey, e);
        throw new StorageException("Error download file from Azure blob: " + e.getMessage(), e.getResponse().getStatusCode(), e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try{
            BlobClient blobClient = blobContainerClient.getBlobClient(objectKey);

            logger.info("Deleting file '{}' from Azure Blob", objectKey);

            blobClient.delete();

            logger.info("Successfully deleted file '{}'", objectKey);
        }catch (HttpResponseException e) {
            logger.error("Failed to delete file '{}' from Azure Blob", objectKey, e);
            throw new StorageException("Error deleting file from Azure blob: " + e.getMessage(), e.getResponse().getStatusCode(), e);
        }

    }
}
