package com.vicente.application.config.storage.s3;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.storage.s3")
@Validated
public record S3StorageProperties(
    @NotBlank
    String url,
    @NotBlank
    String accessKey,
    @NotBlank
    String secretKey,
    @NotBlank
    String bucketName,
    @NotBlank
    String region
){}
