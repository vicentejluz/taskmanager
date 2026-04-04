package com.vicente.storage;

import com.vicente.storage.dto.StorageObject;
import com.vicente.storage.util.StorageLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class S3StorageService implements StorageService {
    // Cliente S3 que será usado para comunicar com o MinIO/S3
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    // Nome do bucket definido no application.properties
    private final String bucketName;

    private static final Logger logger = LoggerFactory.getLogger(S3StorageService.class);

    // Injeção de dependência do S3Client configurado no projeto
    public S3StorageService(S3Client s3Client, S3Presigner s3Presigner, String bucketName) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
        logger.info("S3StorageService initialized with bucket: {}", bucketName);
    }

    @Override
    public void upload(Path file, long contentLength, String objectKey, String mimeType) {
        try {
            logger.info("Uploading file '{}' to S3 bucket '{}' ({} bytes, MIME: {})",
                    objectKey, bucketName, contentLength, mimeType);
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .contentType(mimeType)
                            .contentLength(contentLength)
                            .acl(ObjectCannedACL.PRIVATE)
                            .build(),
                    RequestBody.fromFile(file)
            );
            logger.info("Successfully uploaded file '{}' to S3 bucket '{}'", objectKey, bucketName);
        }catch (S3Exception e){
            throw StorageLogger.logAndCreateException(
                    Level.ERROR,
                    "Failed to upload file '{}' to S3 bucket '{}'",
                    "Error sending file to S3: " + e.getMessage(),
                    e.statusCode(),
                    e,
                    objectKey,
                    bucketName
            );
        }catch (SdkException e){
            throw StorageLogger.logAndCreateException(
                    Level.ERROR,
                    "SDK error while uploading file '{}' to S3 bucket '{}'",
                    "SDK error: " + e.getMessage(),
                    HttpStatusCode.INTERNAL_SERVER_ERROR,
                    e,
                    objectKey,
                    bucketName
            );
        }
    }

    @Override
    public InputStream download(String objectKey) {
        try {
            logger.info("Downloading file '{}' from S3 bucket '{}'", objectKey, bucketName);
            return s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .build(),
                    ResponseTransformer.toInputStream()
            );
        }catch (S3Exception e){
            throw StorageLogger.logAndCreateException(
                    Level.ERROR,
                    "Failed to download file '{}' from S3 bucket '{}'",
                    "Error downloading file from S3: " + e.getMessage(),
                    e.statusCode(),
                    e,
                    objectKey,
                    bucketName
            );
        }catch (SdkException e){
            throw StorageLogger.logAndCreateException(
                    Level.ERROR,
                    "SDK error while downloading file '{}' from S3 bucket '{}'",
                    "SDK error: " + e.getMessage(),
                    HttpStatusCode.INTERNAL_SERVER_ERROR,
                    e,
                    objectKey,
                    bucketName
            );
        }
    }

    @Override
    public String generateSignedUrl(String objectKey, Duration duration, String contentDisposition) {
        try {
            logger.info("Generating signed URL for object | objectKey={} duration={}s contentDisposition={}",
                    objectKey, duration.getSeconds(), contentDisposition);
            String signedUrl = s3Presigner.presignGetObject(
                    GetObjectPresignRequest.builder()
                            .getObjectRequest(
                                    GetObjectRequest.builder()
                                            .bucket(bucketName)
                                            .responseContentDisposition(contentDisposition)
                                            .key(objectKey)
                                            .build())
                            // duração da URL assinada
                            .signatureDuration(duration)
                            .build()
            ).url().toExternalForm();

            logger.info("Signed URL generated successfully | objectKey={} duration={}s contentDisposition={}",
                    objectKey, duration.getSeconds(), contentDisposition);

            return signedUrl;
        }catch (S3Exception e){
            throw StorageLogger.logAndCreateException(
                    Level.ERROR,
                    "Failed to generate signed URL for object '{}' in bucket '{}'",
                    "S3 error while generating signed URL",
                    e.statusCode(),
                    e,
                    objectKey,
                    bucketName
            );
        }catch (SdkException e){
            throw StorageLogger.logAndCreateException(
                    Level.ERROR,
                    "SDK error while generating signed URL for object '{}' in bucket '{}'",
                    "SDK error: " + e.getMessage(),
                    HttpStatusCode.INTERNAL_SERVER_ERROR,
                    e,
                    objectKey,
                    bucketName
            );
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            logger.info("Deleting file '{}' from S3 bucket '{}'", objectKey, bucketName);
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .build()
            );
            logger.info("Successfully deleted file '{}' from S3 bucket '{}'", objectKey, bucketName);
        }catch (S3Exception e){
            throw StorageLogger.logAndCreateException(
                    Level.ERROR,
                    "Failed to delete file '{}' from S3 bucket '{}'",
                    "Error deleting file from S3: " + e.getMessage(),
                    e.statusCode(),
                    e,
                    objectKey,
                    bucketName
            );
        } catch (SdkException e){
            throw StorageLogger.logAndCreateException(
                    Level.ERROR,
                    "SDK error while deleting file '{}' from S3 bucket '{}'",
                    "SDK error: " + e.getMessage(),
                    HttpStatusCode.INTERNAL_SERVER_ERROR,
                    e,
                    objectKey,
                    bucketName
            );
        }
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            logger.info("Checking if file '{}' exists in S3 bucket '{}'", objectKey, bucketName);
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            s3Client.headObject(headObjectRequest);

            logger.info("File '{}' exists in S3 bucket '{}'", objectKey, bucketName);
            return  true;
        }catch (NoSuchKeyException e){
            logger.warn("File '{}' not found in S3 bucket '{}'", objectKey, bucketName);
            return false;
        }catch (S3Exception e){
            throw StorageLogger.logAndCreateException(
                    Level.ERROR,
                    "Failed to check existence of file '{}' in S3 bucket '{}'",
                    "Error checking file existence in S3: " + e.getMessage(),
                    e.statusCode(),
                    e,
                    objectKey,
                    bucketName
            );
        } catch (SdkException e){
            throw StorageLogger.logAndCreateException(
                    Level.ERROR,
                    "SDK error while checking existence of file '{}' in S3 bucket '{}'",
                    "SDK error: " + e.getMessage(),
                    HttpStatusCode.INTERNAL_SERVER_ERROR,
                    e,
                    objectKey,
                    bucketName
            );
        }
    }

    @Override
    public List<StorageObject> list(String path) {
        try {
            logger.info("Listing files from S3 bucket '{}' with prefix '{}'", bucketName, path);
            List<StorageObject> list = new ArrayList<>();
            ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(path)
                    .build();

            ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);

            listObjectsV2Response.contents().forEach(c -> list.add(new StorageObject(c.key(),
                    c.size(), c.lastModified().atOffset(ZoneOffset.UTC))));

            logger.info("Successfully listed {} file(s) from S3 bucket '{}' with prefix '{}'",
                    list.size(), bucketName, path);
            return list;
        }catch (S3Exception e){
            throw StorageLogger.logAndCreateException(
                    Level.ERROR,
                    "Failed to list files from S3 bucket '{}' with prefix '{}'",
                    "Error listing files from S3: " + e.getMessage(),
                    e.statusCode(),
                    e,
                    bucketName,
                    path
            );
        } catch (SdkException e){
            throw StorageLogger.logAndCreateException(
                    Level.ERROR,
                    "SDK error while listing files from S3 bucket '{}' with prefix '{}'",
                    "SDK error: " + e.getMessage(),
                    HttpStatusCode.INTERNAL_SERVER_ERROR,
                    e,
                    bucketName,
                    path
            );
        }
    }
}

/*
 * Arquivo privado: somente o backend pode acessar.
 * Usuários externos não conseguem acessar via URL direta.
 * .acl(ObjectCannedACL.PRIVATE)
 *
 * Arquivo público: qualquer pessoa pode baixar via URL.
 * Muito usado para imagens, avatares ou arquivos públicos.
 * .acl(ObjectCannedACL.PUBLIC_READ)
 *
 * Apenas usuários autenticados no serviço S3 podem acessar o arquivo.
 * .acl(ObjectCannedACL.AUTHENTICATED_READ)
 *
 * Qualquer pessoa pode ler e modificar o arquivo.
 * Extremamente perigoso e raramente usado em produção.
 * .acl(ObjectCannedACL.PUBLIC_READ_WRITE)
 */

/*  // Cria um objeto Path apontando para o arquivo local no sistema de arquivos.
    // Nesse caso, um arquivo .log dentro da pasta ‘Downloads’.
    Path filePath = Paths.get("/Users/vicentejluz/Downloads/taskmanager/logs/taskmanager-7.log");

    // Tenta detectar automaticamente o MIME type do arquivo baseado na extensão
    // e no sistema operacional. Para .log normalmente retorna "text/plain".
    String mimeType = Files.probeContentType(filePath);

    // Envia (upload) o arquivo para o Object Storage (MinIO / S3).
        s3Client.putObject(
        // Cria a requisição de ‘upload’ com as configurações do objeto.
        PutObjectRequest.builder()
                        .bucket(bucketName)
    // Nome (chave) do arquivo dentro do bucket.
    // Esse será o "caminho" do objeto no storage.
                        .key("logs/taskmanager.log")
    // Define o MIME type do arquivo.
    // Isso informa ao storage e aos browsers que tipo de arquivo é.
                        .contentType(mimeType)
    // Define a ACL (Access Control List) do objeto
    // Isso controla quem pode acessar o arquivo diretamente no storage
    // PRIVATE = somente o dono do bucket (backend) pode acessar
    // Esse é o padrão mais seguro para APIs
                        .acl(ObjectCannedACL.PRIVATE)
    // Finaliza a construção do objeto PutObjectRequest.
                        .build(),
    // Define o conteúdo que será enviado para o storage.
    // Aqui o SDK lê diretamente o arquivo do disco e envia os bytes.
                RequestBody.fromFile(filePath)
        );

                System.out.println("Upload realizado com sucesso!");

    // Cria o diretório no projeto caso ele ainda não exista.
    // Se a pasta já existir, o método não faz nada.
    // Isso evita erro ao tentar salvar o arquivo em um diretório inexistente.
        Files.createDirectories(Paths.get("test"));

        // Executa uma operação GET no S3/MinIO para baixar um objeto.
        s3Client.getObject(
        // Cria a requisição para buscar o objeto.
        GetObjectRequest.builder()
    // Nome do bucket onde o arquivo está armazenado.
                        .bucket(bucketName)
    // Caminho (key) do arquivo dentro do bucket.
    // Nesse caso o arquivo está dentro da "pasta" logs.
    // No S3/MinIO pastas são apenas parte do nome do arquivo.
                        .key("logs/taskmanager.log")
    // Finaliza a construção da requisição.
                        .build(),
    // Define como a resposta será processada.
    // Aqui estamos dizendo para salvar o conteúdo recebido num arquivo.

                ResponseTransformer.toFile(
        // Caminho do arquivo local onde o download será salvo.
        Paths.get("test/taskmanager-7.log"))
        );
*/
