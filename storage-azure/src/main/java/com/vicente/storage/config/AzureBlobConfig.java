package com.vicente.storage.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class AzureBlobConfig {
    private final StorageProperties storageProperties;

    public AzureBlobConfig(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }
}
