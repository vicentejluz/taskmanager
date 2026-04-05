package com.vicente.storage;

import com.vicente.storage.dto.StorageObject;
import com.vicente.storage.exception.SignedUrlValidationException;
import com.vicente.storage.exception.StorageException;
import com.vicente.storage.security.LocalStorageHmacTokenGenerator;
import com.vicente.storage.util.StorageLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class LocalStorageServiceImpl implements SecretAwareStorageService {
    private final Path rootPath;
    private static final Logger logger = LoggerFactory.getLogger(LocalStorageServiceImpl.class);

    public LocalStorageServiceImpl(String rootPath) {
        this.rootPath = Paths.get(rootPath).toAbsolutePath().normalize();
        logger.info("LocalStorageService initialized at root path: {}", this.rootPath);
    }

    @Override
    public void upload(Path file, long contentLength, String objectKey, String mimeType) {
        try {
           Path path = rootPath.resolve(objectKey).normalize();

           ensurePathWithinRoot(path);

           Files.createDirectories(path.getParent());

           long sourceSize = Files.size(file);

           Files.copy(file, path);

           logger.info("Uploaded file '{}' ({} bytes, MIME={})", path, sourceSize, mimeType);

           if (contentLength > 0 && sourceSize != contentLength) {
                Files.deleteIfExists(path);
                deleteIfEmptyRecursively(path.getParent(), rootPath);
                logAndThrow(
                    "File size mismatch for file '{}', expected {}, got {}",
                    "File size mismatch",
                    400,
                    objectKey, contentLength, sourceSize
                );
           }
        } catch (IOException e) {
            logAndThrow(
                    "Failed to upload file '{}'",
                    "Failed to upload file: " + objectKey + " - " + e.getMessage(),
                    e,
                    objectKey
            );
        }
    }

    @Override
    public InputStream download(String objectKey) {
        try {
            Path path = rootPath.resolve(objectKey).normalize();

            ensurePathWithinRoot(path);

            if(!Files.exists(path)) {
                logAndThrow(
                        "File does not exist: {}",
                        "File not found: " + objectKey,
                        404,
                        path
                );
            }

            logger.info("Downloading file '{}'", path);
            return Files.newInputStream(path, StandardOpenOption.READ);

        } catch (IOException e) {
            throw StorageLogger.logAndCreateException(
                    Level.ERROR,
                    "Failed to download file '{}'",
                    "Failed to download file: " + objectKey + " - " + e.getMessage(),
                    500,
                    e,
                    objectKey
            );
        }
    }

    /**
     * Gera uma URL assinada para o arquivo no LocalStorage.
     * <p>
     * ⚠️ Segurança:
     * <p>
     * - O token é sensível e garante acesso ao arquivo, portanto sempre transmitir a URL via HTTPS.
     * <p>
     * - O token expira de acordo com o parâmetro {@code duration}.
     * <p>
     * - Não compartilhe a URL em canais inseguros.
     *
     * @param objectKey name of the file in storage
     * @param duration validity duration of the URL
     * @param contentDisposition value for Content-Disposition
     * @param secret secret used to generate the HMAC
     * @return signed URL containing the token, expiration, and contentDisposition
     * @throws StorageException if the secret is missing or there are internal HMAC issues
     */
    @Override
    public String generateSignedUrl(String objectKey, Duration duration, String contentDisposition, String secret) {
        if (secret == null || secret.isBlank()) {
            logAndThrow(
                    "LocalStorageService secret is missing or empty. Cannot generate signed URL.",
                    "Secret for LocalStorage is not configured. Check your application properties or environment variables.",
                    500
            );
        }

        Path path = rootPath.resolve(objectKey).normalize();

        ensurePathWithinRoot(path);

        if(!Files.exists(path)) {
            logAndThrow(
                    "File does not exist: {}",
                    "File not found: " + objectKey,
                    404,
                    path
            );
        }

        logger.info("Generating signed URL for local storage | objectKey={} | duration={}s",
                objectKey, duration.getSeconds());

        long expireAt = OffsetDateTime.now().plus(duration).toEpochSecond();

        String storageFileName = path.getFileName().toString();

        String encodedContentDisposition = urlEncoder(contentDisposition);

        String data = storageFileName + ":" + expireAt + ":" + encodedContentDisposition;

        String token = LocalStorageHmacTokenGenerator.generateHmac256(secret.trim(), data);

        logger.info("Signed URL generated successfully | objectKey={} | expireAt={}",
                objectKey, expireAt);

        return storageFileName +
                "?exp=" + expireAt +
                "&token=" + token +
                "&rscd=" + encodedContentDisposition;
    }

    /**
     * Valida uma URL assinada para um arquivo no LocalStorage.
     * <p>
     * ⚠️ Segurança:
     * <p>
     * - O token é sensível e garante acesso ao arquivo, portanto sempre transmita via HTTPS.
     * <p>
     * - O token não deve estar expirado.
     * <p>
     * - Não compartilhe a URL assinada em canais inseguros.
     *
     * @param token the HMAC token from the signed URL
     * @param storageFileName name of the file in storage
     * @param expireAt expiration time of the URL (epoch seconds)
     * @param contentDisposition value for Content-Disposition
     * @param secret secret used to generate the HMAC
     * @throws SignedUrlValidationException if the token is invalid, expired, or the secret is missing
     */
    @Override
    public void validateSignedUrl(String token, String storageFileName, long expireAt, String contentDisposition, String secret) {
        if (secret == null || secret.isBlank()) {
            logger.debug("LocalStorageService secret is missing or empty. Cannot validate signed URL.");

            throw new SignedUrlValidationException(
                    "Secret for LocalStorage is not configured. Check your application properties or environment variables.",
                    500
            );
        }

        validateExpiration(storageFileName, expireAt);

        String data = buildData(storageFileName, expireAt, contentDisposition);

        validateHmacToken(token, data, storageFileName, secret.trim());
    }

    @Override
    public void delete(String objectKey) {
        try {
            Path path = rootPath.resolve(objectKey).normalize();

            ensurePathWithinRoot(path);

            Files.deleteIfExists(path);

            if(Files.exists(path)){
                logAndThrow(
                        "File '{}' exists but could not be deleted",
                        "File exists but could not be deleted: " + path,
                        500,
                        path
                );
            }

            deleteIfEmptyRecursively(path.getParent(), rootPath);
            logger.info("Deleted file '{}'", path);
        }catch (IOException e) {
            logAndThrow(
                    "Failed to delete file '{}'",
                    "Failed to delete file: " + objectKey + " - " + e.getMessage(),
                    e,
                    objectKey
            );
        }
    }

    @Override
    public boolean exists(String objectKey) {
        Path path = rootPath.resolve(objectKey).normalize();

        ensurePathWithinRoot(path);

        logger.info("Checking if local file exists | path={}", path);

        // Files.exists(path) -> verifica se o caminho realmente existe no sistema de arquivos
        // Files.isRegularFile(path) -> garante que o caminho é um arquivo "normal"
        // (não é diretório, não é link simbólico, não é dispositivo, etc.)
        // Usamos os dois juntos para evitar retornar true quando o path existir,
        // mas for uma pasta em vez de um arquivo.
        boolean exists = Files.exists(path) && Files.isRegularFile(path);

        if (exists) {
            logger.info("Local file exists | path={}", path);
        } else {
            logger.warn("Local file not found | path={}", path);
        }

        return exists;
    }

    @Override
    public List<StorageObject> list(String path) {
        try {
            // Resolve o caminho relativo recebido para dentro do root do storage
            // normalize() remove ".." e "." evitando path traversal
            Path directory = rootPath.resolve(path).normalize();

            // Lista que vai armazenar os objetos retornados
            List<StorageObject> storageObjects = new ArrayList<>();

            // Garante que o path resolvido continua dentro do diretório root
            // proteção contra acesso fora do storage (ex: ../../etc/passwd)
            ensurePathWithinRoot(directory);

            logger.info("Listing local files | directory={}", directory);

            // Verifica se o diretório existe e se realmente é um diretório
            // evita erro caso seja arquivo ou path inválido
            if (!Files.exists(directory) || !Files.isDirectory(directory)) {
                logAndThrow(
                        "Directory not found in local storage | directory={}",
                        "Directory not found in local storage: " + path,
                        404,
                        directory
                );
            }

            // Files.list abre um stream que precisa ser fechado
            // try-with-resources garante fechamento automático
            try (Stream<Path> stream = Files.list(directory)) {

                stream
                        // Filtra apenas arquivos, ignorando subdiretórios
                        .filter(Files::isRegularFile)

                        // Itera sobre cada arquivo encontrado
                        .forEach(file -> {
                            try {
                                storageObjects.add(
                                        new StorageObject(

                                                // Converte path absoluto para relativo ao root
                                                // mantém consistência com S3/Azure
                                                rootPath.relativize(file).toString(),

                                                // Obtém tamanho do arquivo em bytes
                                                Files.size(file),

                                                // Obtém data de modificação e converte para OffsetDateTime UTC
                                                Files.getLastModifiedTime(file)
                                                        .toInstant()
                                                        .atOffset(ZoneOffset.UTC)
                                        )
                                );

                            } catch (IOException e) {
                                // IOException não pode ser lançada diretamente no stream
                                // então convertemos para UncheckedIOException
                                logger.error("Error reading local file metadata | file={} directory={}", file, directory, e);
                                throw new UncheckedIOException(e);
                            }
                        });

                logger.info("Successfully listed {} file(s) from local storage | directory={}",
                        storageObjects.size(), directory);

                return storageObjects;

            } catch (UncheckedIOException e) {
                // Captura erro lançado dentro do stream
                // Converte novamente para sua exception padrão
                throw StorageLogger.logAndCreateException(
                        Level.ERROR,
                        "Error reading file metadata from local storage | directory={}",
                        "Error reading file metadata from local storage: " + path,
                        500,
                        e,
                        directory
                );
            }

        } catch (IOException e) {
            // Erros gerais de IO (ex: permission denied, disk error, etc)
            throw StorageLogger.logAndCreateException(
                    Level.ERROR,
                    "I/O error while listing local storage | path={}",
                    "I/O error while listing local storage: " + path,
                    500,
                    e,
                    path
            );
        }
    }

    private void ensurePathWithinRoot(Path path) {
        if (!path.startsWith(rootPath)) {
            logger.debug("Invalid path: {}", path);
            throw new StorageException("Invalid path", 400);
        }
    }

    private void deleteIfEmptyRecursively(Path path, Path root) throws IOException {
        // 1. Condição de parada: não subir além da root ou se o caminho não existir
        if (path == null || !path.startsWith(root) || path.equals(root)) {
            return;
        }

        // 2. Verifica se a pasta atual está vazia
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            if (!stream.iterator().hasNext()) {
                // Está vazia, então deleta
                Files.delete(path);
                logger.info("Deleted empty directory '{}'", path);

                // 3. Tenta deletar a pasta pai (sobe um nível)
                deleteIfEmptyRecursively(path.getParent(), root);
            } else {
                logger.debug("Directory not empty, skipping deletion: {}", path);
            }
        }
    }

    private void logAndThrow(String msgLog, String msgThrow, int statusCode, Object... args) {
        StorageLogger.logAndThrow(Level.DEBUG, msgLog, msgThrow, statusCode, args);
    }

    private void logAndThrow(String msgLog, String msgThrow, Throwable e, Object... args) {
        StorageLogger.logAndThrow(Level.ERROR, msgLog, msgThrow, 500, e, args);
    }

    private static void validateExpiration(String storageFileName, long expireAt) {
        if (expireAt <= 0) {
            throw new SignedUrlValidationException(
                    "Signed URL expiration timestamp is invalid for file '" + storageFileName + "' | expireAt=" + expireAt,
                    403
            );
        }

        Instant now = Instant.now();
        Instant expireInstant = Instant.ofEpochSecond(expireAt);

        if (!now.isBefore(expireInstant)) {
            logger.debug("Signed URL expired | now={} | expireAt={}", now, expireInstant);

            String msg = String.format("Signed URL expired for file '%s'. Expiration time: %s",
                    storageFileName, expireInstant.atOffset(ZoneOffset.UTC));

            throw new SignedUrlValidationException(msg, 403);
        }

        logger.info("Signed URL expiration valid | now={} | expireAt={}", now, expireInstant);
    }

    private String buildData(String storageFileName, long expireAt, String contentDisposition) {
        String encodedContentDisposition = urlEncoder(contentDisposition);
        return storageFileName + ":" + expireAt + ":" + encodedContentDisposition;
    }

    private void validateHmacToken(String token, String data, String storageFileName, String secret) {
        String expectedToken;
        try {
            // Gera o HMAC esperado para os dados fornecidos
            expectedToken = LocalStorageHmacTokenGenerator.generateHmac256(secret, data);
        }catch (Exception e) {
            logger.error("Error generating HMAC token", e);
            throw new SignedUrlValidationException("Error generating HMAC token for signed URL", 500, e);
        }

        // Compara tokens de forma segura em tempo constante
        if(!LocalStorageHmacTokenGenerator.secureCompare(expectedToken, token)) {
            logger.debug("Invalid token provided for signed URL | storageFileName={}", storageFileName);
            throw new SignedUrlValidationException("Invalid token for signed URL | file=" + storageFileName, 403);
        }

        logger.info("Token validated successfully");
    }

    private String urlEncoder(String contentDisposition) {
        return URLEncoder.encode(contentDisposition, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}
