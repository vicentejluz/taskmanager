package com.vicente.storage.config;

import java.net.URI;

import com.vicente.storage.StorageService;
import com.vicente.storage.S3StorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
@EnableConfigurationProperties(S3StorageProperties.class)
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3", matchIfMissing = true)
public class S3StorageConfig {
    @Bean
    public S3Client s3Client(S3StorageProperties s3StorageProperties) {
        // Cria um objeto de credenciais usando Access Key e Secret Key.
        AwsCredentials credentials = AwsBasicCredentials.create(
                s3StorageProperties.accessKey(), s3StorageProperties.secretKey());
        // Essas credenciais são usadas pelo cliente S3 para autenticar nas requisições.
        return S3Client
                .builder()
                // Define uma região para o cliente S3.
                // O SDK exige uma região mesmo quando usamos MinIO.
                // MinIO, na prática, ignora a região, mas o SDK precisa de um valor.
                .region(Region.of(s3StorageProperties.region()))
                // Sobrescreve o endpoint padrão da AWS.
                // Em vez de enviar requisições para a AWS, o cliente vai se conectar
                // ao seu servidor MinIO rodando localmente.
                .endpointOverride(URI.create(s3StorageProperties.url()))
                // Define o provedor de credenciais que o cliente usará.
                // StaticCredentialsProvider significa que as credenciais são fixas
                // e não serão renovadas automaticamente.
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                // Configuração específica do protocolo S3.
                .serviceConfiguration(S3Configuration
                        .builder()
                        // Habilita o "path-style access".
                        // Isso faz com que as URLs fiquem assim:
                        // http://localhost:9000/bucket/arquivo
                        //
                        // Sem isso o SDK tenta usar o estilo virtual-host:
                        // http://bucket.localhost:9000/arquivo
                        //
                        // Esse formato normalmente não funciona com MinIO local.
                        .pathStyleAccessEnabled(true)
                        // Finaliza a configuração do S3Configuration.
                        .build())
                // Constrói e retorna o cliente S3 configurado.
                .build();
    }

    @Bean
    public StorageService s3Storage(S3Client s3Client, S3StorageProperties s3StorageProperties) {
        return new S3StorageService(s3Client, s3StorageProperties.bucketName());
    }
}
