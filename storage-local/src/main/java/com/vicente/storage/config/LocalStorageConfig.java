package com.vicente.storage.config;

import com.vicente.storage.LocalStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageConfig {

    @Bean
    public LocalStorageService localStorageService(@Value("${app.storage.local.path:./local-storage}") String path,
                                                   @Value("${app.storage.local.secret-key}") String secret) {
        return new LocalStorageService(path, secret);
    }
}
