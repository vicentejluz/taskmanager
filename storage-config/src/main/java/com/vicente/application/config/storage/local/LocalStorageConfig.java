package com.vicente.application.config.storage.local;

import com.vicente.storage.LocalStorageServiceImpl;
import com.vicente.storage.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageConfig {

    @Bean
    public StorageService localStorageImpl(@Value("${app.storage.local.path:./local-storage}") String path) {
        return new LocalStorageServiceImpl(path);
    }
}
