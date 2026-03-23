package com.vicente.storage;

import com.vicente.storage.exception.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

public class LocalStorageService implements StorageService {
    private final Path rootPath;
    private static final Logger logger = LoggerFactory.getLogger(LocalStorageService.class);

    public LocalStorageService(String rootPath) {
        this.rootPath = Paths.get(rootPath).toAbsolutePath().normalize();
        logger.info("LocalStorageService initialized at root path: {}", this.rootPath);
    }

    @Override
    public void upload(InputStream file, long contentLength, String objectKey, String mimeType) {
        try {
           Path path = rootPath.resolve(objectKey).normalize();

            ensurePathWithinRoot(path);

           Files.createDirectories(path.getParent());

            try(InputStream in = file) {
                long copied = Files.copy(in, path);
                logger.info("Uploaded file '{}' ({} bytes)", path, copied);

                if (contentLength > 0 && copied != contentLength) {
                    Files.deleteIfExists(path);
                    deleteIfEmptyRecursively(path.getParent(), rootPath);
                    logger.debug("File size mismatch, expected {}, got {}", contentLength, copied);
                    throw new StorageException("File size mismatch", 400);
                }
            }

        } catch (IOException e) {
            logger.error("Failed to upload file '{}'", objectKey, e);
            throw new StorageException("Failed to upload file: " + objectKey + " - " + e.getMessage(), 500, e);
        }
    }

    @Override
    public InputStream download(String objectKey) {
        try {
            Path path = rootPath.resolve(objectKey).normalize();

            ensurePathWithinRoot(path);

            if(!Files.exists(path)) {
                logger.debug("File does not exist: {}", path);
                throw new StorageException("File not found", 404);
            }

            logger.info("Downloading file '{}'", path);
            return Files.newInputStream(path, StandardOpenOption.READ);

        } catch (IOException e) {
            logger.error("Failed to download file '{}'", objectKey, e);
            throw new StorageException("Failed to download file: " + objectKey + " - " + e.getMessage(), 500, e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Path path = rootPath.resolve(objectKey).normalize();

            ensurePathWithinRoot(path);

            Files.deleteIfExists(path);

            if(Files.exists(path)){
                logger.debug("File '{}' exists but could not be deleted", path);
                throw new StorageException("File exists but could not be deleted: " + path, 500);
            }

            deleteIfEmptyRecursively(path.getParent(), rootPath);
            logger.info("Deleted file '{}'", path);
        }catch (IOException e) {
            logger.error("Failed to delete file '{}'", objectKey, e);
            throw new StorageException("Failed to delete file: " + objectKey + " - " + e.getMessage(), 500, e);
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
}
