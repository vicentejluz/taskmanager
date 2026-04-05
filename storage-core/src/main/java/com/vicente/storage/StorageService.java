package com.vicente.storage;

import com.vicente.storage.dto.StorageObject;
import com.vicente.storage.exception.StorageException;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public interface StorageService {
    void upload(Path file, long contentLength, String objectKey, String mimeType);
    InputStream download(String objectKey);
    void delete(String objectKey);
    boolean exists(String objectKey);
    List<StorageObject> list(String path);

    /**
     * Gera uma URL assinada para um arquivo.
     * <p>
     * Observação: Nem todas as implementações de storage suportam URLs assinadas.
     * Por padrão, este método lança uma {@link StorageException}.
     * <p>
     * ⚠️ Segurança:
     * <p>
     * - A URL assinada pode conceder acesso direto ao arquivo, portanto sempre transmita via HTTPS.
     * <p>
     * - Nem todos os backends de storage implementam este método; verifique a implementação específica.
     *
     * @param objectKey the name of the file in storage
     * @param duration validity duration of the signed URL
     * @param contentDisposition value for the Content-Disposition header when downloading
     * @return signed URL to download the file
     * @throws StorageException if the storage implementation does not support signed URLs or an error occurs
     */
    default String generateSignedUrl(String objectKey, Duration duration, String contentDisposition){
        throw new StorageException("generateSignedUrl is not implemented for this storage", 500);
    }

    default String buildObjectKey(String path, String storedFileName) {
        return path.replaceAll("/+$", "") + "/" + storedFileName;
    }
}
