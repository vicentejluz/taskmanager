package com.vicente.storage;

import com.vicente.storage.exception.StorageException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;

public class S3StorageService implements StorageService {
    // Cliente S3 que será usado para comunicar com o MinIO/S3
    private final S3Client s3Client;

    // Nome do bucket definido no application.properties
    private final String bucketName;

    // Injeção de dependência do S3Client configurado no projeto
    public S3StorageService(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public void upload(InputStream file, long contentLength, String objectKey, String mimeType) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .contentType(mimeType)
                            .acl(ObjectCannedACL.PRIVATE)
                            .build(),
                    RequestBody.fromInputStream(file, contentLength)
            );
        }catch (S3Exception e){
            throw new StorageException("Error sending file to S3: " + e.getMessage(), e.statusCode(), e);
        }catch (SdkException e){
            throw new StorageException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    public InputStream download(String objectKey) {
        try {
            return s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .build(),
                    ResponseTransformer.toInputStream()
            );
        }catch (S3Exception e){
            throw new StorageException("Error download file from S3: " + e.getMessage(), e.statusCode(), e);
        }catch (SdkException e){
            throw new StorageException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .build()
            );
        }catch (S3Exception e){
            throw new StorageException("Error deleting file from S3: " + e.getMessage(), e.statusCode(), e);
        } catch (SdkException e){
            throw new StorageException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR, e);
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

//// Cria um objeto Path apontando para o arquivo local no sistema de arquivos.
//// Nesse caso, um arquivo .log dentro da pasta ‘Downloads’.
//Path filePath = Paths.get("/Users/vicentejluz/Downloads/taskmanager/logs/taskmanager-7.log");
//
//// Tenta detectar automaticamente o MIME type do arquivo baseado na extensão
//// e no sistema operacional. Para .log normalmente retorna "text/plain".
//String mimeType = Files.probeContentType(filePath);
//
//// Envia (upload) o arquivo para o Object Storage (MinIO / S3).
//        s3Client.putObject(
//        // Cria a requisição de ‘upload’ com as configurações do objeto.
//        PutObjectRequest.builder()
//                        .bucket(bucketName)
//// Nome (chave) do arquivo dentro do bucket.
//// Esse será o "caminho" do objeto no storage.
//                        .key("logs/taskmanager.log")
//// Define o MIME type do arquivo.
//// Isso informa ao storage e aos browsers que tipo de arquivo é.
//                        .contentType(mimeType)
//// Define a ACL (Access Control List) do objeto
//// Isso controla quem pode acessar o arquivo diretamente no storage
//// PRIVATE = somente o dono do bucket (backend) pode acessar
//// Esse é o padrão mais seguro para APIs
//                        .acl(ObjectCannedACL.PRIVATE)
//// Finaliza a construção do objeto PutObjectRequest.
//                        .build(),
//// Define o conteúdo que será enviado para o storage.
//// Aqui o SDK lê diretamente o arquivo do disco e envia os bytes.
//                RequestBody.fromFile(filePath)
//        );
//
//                System.out.println("Upload realizado com sucesso!");
//
//// Cria o diretório no projeto caso ele ainda não exista.
//// Se a pasta já existir, o método não faz nada.
//// Isso evita erro ao tentar salvar o arquivo em um diretório inexistente.
//        Files.createDirectories(Paths.get("test"));
//
//        // Executa uma operação GET no S3/MinIO para baixar um objeto.
//        s3Client.getObject(
//        // Cria a requisição para buscar o objeto.
//        GetObjectRequest.builder()
//// Nome do bucket onde o arquivo está armazenado.
//                        .bucket(bucketName)
//// Caminho (key) do arquivo dentro do bucket.
//// Nesse caso o arquivo está dentro da "pasta" logs.
//// No S3/MinIO pastas são apenas parte do nome do arquivo.
//                        .key("logs/taskmanager.log")
//// Finaliza a construção da requisição.
//                        .build(),
//// Define como a resposta será processada.
//// Aqui estamos dizendo para salvar o conteúdo recebido num arquivo.
//
//                ResponseTransformer.toFile(
//        // Caminho do arquivo local onde o download será salvo.
//        Paths.get("test/taskmanager-7.log"))
//        );
