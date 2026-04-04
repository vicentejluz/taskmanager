package com.vicente.storage;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.vicente.storage.dto.StorageObject;
import com.vicente.storage.exception.StorageException;
import com.vicente.storage.util.StorageLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.io.*;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
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
    public void upload(Path file, long contentLength, String objectKey, String mimeType) {
        try {
            // Obtém um cliente específico para o blob que vamos criar/atualizar no container
            // objectKey = caminho/nome do arquivo dentro do container
            BlobClient blobClient = blobContainerClient.getBlobClient(objectKey);

            logger.info("Uploading file '{}' to Azure Blob ({} bytes, MIME: {})", objectKey, contentLength, mimeType);

            // Cria uma instância de BinaryData a partir do arquivo local
            // fromFile(file) indica:
            // BinaryData é um wrapper que o SDK Azure usa para ler o arquivo de forma eficiente em chunks
            // BlobParallelUploadOptions permite configurar o upload, incluindo:
            // - headers (como Content-Type)
            // - chunk size (internamente usa padrão de 4MB, se não configurado)
            // - opções de paralelismo e retry (internamente gerenciadas pelo SDK)
            blobClient.uploadWithResponse(new BlobParallelUploadOptions(BinaryData.fromFile(file))
                            .setHeaders(new BlobHttpHeaders().setContentType(mimeType)), // Define o tipo MIME do arquivo no blob
                    null, // Timeout da operação: se definido, limita quanto tempo o upload pode levar antes de expirar. Aqui é null, então será usado o valor padrão do SDK.
                    null); // Contexto do pipeline: permite passar informações adicionais para tracing, logging ou telemetria. Aqui é null porque não usamos contexto extra.

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


    /**
     * Gera uma URL assinada (SAS) para download de um blob no Azure Storage, permitindo que
     * o arquivo seja baixado com um nome definido pelo usuário (contentDisposition).
     *
     * @param objectKey The blob name in the container (usually the UUID saved in the database)
     * @param duration The validity duration of the signed URL
     * @param contentDisposition The file name that will appear when downloading (e.g., "myfile.pdf")
     * @return Signed URL to download the blob
     * @throws StorageException Thrown if any error occurs while generating the SAS URL
     */
    @Override
    public String generateSignedUrl(String objectKey, Duration duration, String contentDisposition) {
        try{
            // Obtém o BlobClient para o blob específico dentro do container
            // BlobClient permite operações individuais no blob (download, upload, SAS etc.)
            BlobClient blobClient = blobContainerClient.getBlobClient(objectKey);

            // Define o momento em que a SAS irá expirar
            OffsetDateTime expiryTime = OffsetDateTime.now().plus(duration);

            // Cria permissões para a SAS (nesse caso, apenas leitura)
            BlobSasPermission sasPermission = new BlobSasPermission().setReadPermission(true);

            // Cria os valores da SAS, incluindo:
            // - tempo de expiração
            // - permissões
            // - contentDisposition: nome do arquivo que aparecerá no download
            // - startTime: início da validade da SAS (compensa diferenças de horário, clock skew)
            BlobServiceSasSignatureValues sasSignatureValues = new BlobServiceSasSignatureValues(
                    expiryTime, sasPermission)
                    .setContentDisposition(contentDisposition)
                    .setStartTime(OffsetDateTime.now().minusMinutes(5L));

            // Gera a SAS para o blob usando os valores configurados acima
            String sasToken = blobClient.generateSas(sasSignatureValues);

            // Log informativo: URL sendo gerada, tempo de validade e nome do arquivo
            logger.info("Generating signed URL for Azure blob | objectKey={} duration={}s contentDisposition={}",
                    objectKey, duration.getSeconds(), contentDisposition);

            // Retorna a URL final: URL do blob + SAS token
            // Essa URL pode ser usada diretamente no front para download
            return blobClient.getBlobUrl() + "?" + sasToken;

        }catch (HttpResponseException e) {
            // Se houver erro na comunicação com o Azure, loga e cria exceção customizada
            throw StorageLogger.logAndCreateException(
                    Level.ERROR,
                    "Failed to generate SAS URL for blob '{}'",
                    "Error generating SAS URL for Azure blob: " + e.getMessage(),
                    e.getResponse().getStatusCode(),
                    e,
                    objectKey
            );
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
