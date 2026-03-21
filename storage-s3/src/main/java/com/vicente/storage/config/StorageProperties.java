package com.vicente.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.s3")
public class StorageProperties {
    private String url;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String region;

    public String getUrl() {
        return url;
    }

    public String getRegion() {
        return region;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
