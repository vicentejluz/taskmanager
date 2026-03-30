package com.vicente.storage;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.vicente.storage.dto.StorageObject;
import com.vicente.storage.util.StorageLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

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
            throw StorageLogger.logAndCreateException(Level.ERROR, "Failed to upload file '{}' to Azure Blob",
                    "Error sending file to Azure blob: " + e.getMessage(),
                    e.getResponse().getStatusCode(), e, objectKey);
        }
    }

    @Override
    public InputStream download(String objectKey) {
        try{
            BlobClient blobClient = blobContainerClient.getBlobClient(objectKey);

            logger.info("Downloading file '{}' from Azure Blob", objectKey);

            return blobClient.openInputStream();
        }catch (HttpResponseException e) {
            throw StorageLogger.logAndCreateException(Level.ERROR, "Failed to download file '{}' from Azure Blob",
                    "Error download file from Azure blob: " + e.getMessage(),
                    e.getResponse().getStatusCode(), e, objectKey);
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
            throw StorageLogger.logAndCreateException(Level.ERROR, "Failed to delete file '{}' from Azure Blob",
                    "Error deleting file from Azure blob: " + e.getMessage(),
                    e.getResponse().getStatusCode(), e, objectKey);
        }

    }

    public boolean exists(String objectKey) {
        try{
            BlobClient blobClient = blobContainerClient.getBlobClient(objectKey);
            return blobClient.exists();
        }catch (HttpResponseException e) {
            throw StorageLogger.logAndCreateException(Level.ERROR, "Failed to check if file '{}' in Azure Blob",
                    "Failed to check if file in Azure Blob: " + e.getMessage(),
                    e.getResponse().getStatusCode(), e, objectKey);
        }
    }

    @Override
    public List<StorageObject> list(String path) {
        try {
            List<StorageObject> list = new ArrayList<>();
            ListBlobsOptions listBlobsOptions = new ListBlobsOptions().setPrefix(path);
            blobContainerClient.listBlobs(listBlobsOptions, null).forEach(blob ->
                    list.add(new StorageObject(
                            blob.getName(),
                            blob.getProperties().getContentLength(),
                            blob.getProperties().getLastModified()
                    )));

            return list;
        }catch (HttpResponseException e) {
           throw StorageLogger.logAndCreateException(Level.ERROR, "Failed to list files in Azure Blob | path={}",
                    "Failed to list files in Azure Blob: " + e.getMessage(),
                    e.getResponse().getStatusCode(), e, path);
        }
    }
}
