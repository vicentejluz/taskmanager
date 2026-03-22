package com.vicente.storage.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.storage.azure-blob")
@Validated
public record AzureBlobStorageProperties(
        String endpoint,
        @NotBlank
        String containerName,
        String accountName,
        String accountKey,
        String connectionString
) {
        @AssertTrue(message = "Either connectionString OR endpoint/accountName/accountKey must be provided")
        public boolean isValid() {
                return hasConnectionString() || hasSharedKey();
        }

        public boolean hasSharedKey() {
            return endpoint != null && !this.endpoint.isBlank()
                    && this.accountName != null && !this.accountName.isBlank()
                    && this.accountKey != null && !this.accountKey.isBlank();
        }

        public boolean hasConnectionString() {
            return this.connectionString != null && !this.connectionString.isBlank();
        }
}
