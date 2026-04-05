package com.vicente.application.config.storage.azure;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.vicente.storage.AzureBlobStorageServiceImpl;
import com.vicente.storage.StorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AzureBlobStorageProperties.class)
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "azure")
public class AzureBlobConfig {

    @Bean
    public BlobServiceClient blobServiceClient(AzureBlobStorageProperties azureBlobStorageProperties) {
        BlobServiceClientBuilder builder = new BlobServiceClientBuilder();

        if(azureBlobStorageProperties.hasConnectionString()) {
            return builder.connectionString(azureBlobStorageProperties.connectionString()).buildClient();
        }

        return builder
                .endpoint(azureBlobStorageProperties.endpoint())
                .credential(new StorageSharedKeyCredential(
                        azureBlobStorageProperties.accountName(),
                        azureBlobStorageProperties.accountKey()))
                .buildClient();
    }

    @Bean
    public BlobContainerClient blobContainerClient(BlobServiceClient blobServiceClient,
                                                   AzureBlobStorageProperties azureBlobStorageProperties) {
        BlobContainerClient blobContainerClient = blobServiceClient
                .getBlobContainerClient(azureBlobStorageProperties.containerName());

        blobContainerClient.createIfNotExists();

        return blobContainerClient;
    }

    @Bean
    public StorageService azureBlobStorageImpl(BlobContainerClient blobContainerClient){
        return new AzureBlobStorageServiceImpl(blobContainerClient);
    }
}
