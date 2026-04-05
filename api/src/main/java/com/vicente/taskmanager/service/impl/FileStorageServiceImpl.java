package com.vicente.taskmanager.service.impl;

import com.vicente.storage.StorageService;
import com.vicente.storage.exception.StorageException;
import com.vicente.storage.util.StorageLogger;
import com.vicente.taskmanager.cache.FileShareCacheEntry;
import com.vicente.taskmanager.cache.FileShareUrlCache;
import com.vicente.taskmanager.domain.entity.FileMetadata;
import com.vicente.taskmanager.domain.entity.Task;
import com.vicente.taskmanager.domain.enums.FileExtension;
import com.vicente.taskmanager.domain.enums.TaskStatus;
import com.vicente.taskmanager.dto.internal.FileDownloadResult;
import com.vicente.taskmanager.dto.response.FileStorageDownloadUrlResponseDTO;
import com.vicente.taskmanager.dto.response.FileStorageRenameResponseDTO;
import com.vicente.taskmanager.dto.response.FileStorageResponseDTO;
import com.vicente.taskmanager.exception.TaskStatusNotAllowedException;
import com.vicente.taskmanager.mapper.FileStorageMapper;
import com.vicente.taskmanager.service.FileMetadataService;
import com.vicente.taskmanager.service.FileStorageService;
import com.vicente.taskmanager.service.LocalSignedFileService;
import com.vicente.taskmanager.service.TaskService;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {
    private final StorageService storageService;
    private final FileMetadataService fileMetadataService;
    private final TaskService taskService;
    private final Optional<LocalSignedFileService> localSignedFileService;
    private final FileShareUrlCache fileShareUrlCache;
    private final Tika tika;
    private final DataSize maxFileSize;
    private final Duration signedUrlExpiration;
    private final String storageType;
    private static final TikaConfig TIKA_CONFIG = TikaConfig.getDefaultConfig();
    private static final Logger logger = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    public FileStorageServiceImpl(StorageService storageService, FileMetadataService fileMetadataService,
                                  TaskService taskService, Optional<LocalSignedFileService> localSignedFileService,
                                  FileShareUrlCache fileShareUrlCache, @Value("${file.upload.max-size}") DataSize maxFileSize,
                                  @Value("${app.storage.signed-url-expiration}") Duration signedUrlExpiration, @Value("${app.storage.type:local}") String storageType) {
        this.storageService = storageService;
        this.fileMetadataService = fileMetadataService;
        this.taskService = taskService;
        this.localSignedFileService = localSignedFileService;
        this.fileShareUrlCache = fileShareUrlCache;
        this.maxFileSize = maxFileSize;
        this.signedUrlExpiration = signedUrlExpiration;
        this.storageType = storageType;
        this.tika = new Tika();
    }

    @Override
    public FileStorageResponseDTO upload(MultipartFile file, Long taskId, Long userId) {
        validateUploadRequest(file, taskId, userId);

        long contentLength = file.getSize();

        validateFileSize(contentLength);

        validateMaxFilesLimit(taskId);

        validateTotalSizeLimit(taskId, contentLength);

        Task task = taskService.findByIdAndUserId(taskId, userId);

        validateTaskStatusForFileOperation(task);

        String mimeType = detectMimeType(file);

        try(InputStream inputStream = file.getInputStream()) {

            logger.info("Start uploading file | taskId={}, userId={}, originalFileName={}, size={}",
                    taskId, userId, file.getOriginalFilename(), file.getSize());

            String uuid = UUID.randomUUID().toString();

            String validFileName = prepareFileName(uuid, file.getOriginalFilename(), mimeType);

            String extension = getExtension(validFileName);

            String path = buildPath(userId, taskId);

            validateAllowedExtension(extension);

            validateExtensionMatchesContent(mimeType, extension);

            String storedFileName = buildStoredFileName(uuid, extension);

            String objectKey = storageService.buildObjectKey(path, storedFileName);

            return handleFileUpload(validFileName, path, storedFileName, extension, mimeType, task,
                    inputStream, contentLength, objectKey);
        } catch (IOException e) {
            throw StorageLogger.logAndCreateException(
                    Level.ERROR,
                    "I/O error while uploading file: {}",
                    "Failed to upload file: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    e,
                    e.getMessage()
            );
        } catch (MimeTypeException e) {
            throw StorageLogger.logAndCreateException(
                    Level.WARN,
                    "Invalid mime type detected while uploading file: {}",
                    "Error validating file media type and consistency: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST.value(),
                    e,
                    e.getMessage()
            );

        }catch (IllegalStateException e){
             throw StorageLogger.logAndCreateException(
                    Level.ERROR,
                    "Error uploading file: {}",
                    "Failed to upload file: " + e.getMessage(),
                     HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    e,
                    e.getMessage());
        }
    }

    @Override
    @Transactional
    public FileStorageRenameResponseDTO rename(Long id, Long userId, String newFileName) {
        logger.info("Rename request received | id={}, userId={}, newFileName={}", id, userId, newFileName);
        validateFileNameNotBlank(newFileName);

        validateOwnership(id, userId);

         newFileName = sanitizeFileName(newFileName);

        validateSanitizedFileName(newFileName);

        newFileName = fileMetadataService.update(id, newFileName);

        fileShareUrlCache.removeShareUrl(id);

        logger.info("File renamed successfully | id={}, newFileName={}", id, newFileName);

        return new FileStorageRenameResponseDTO(id, newFileName);
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownloadResult download(Long id, Long userId) {
        logger.info("Start downloading file | id={}", id);
        FileMetadata fileMetadata = validateOwnership(id, userId);

        String objectKey = storageService.buildObjectKey(fileMetadata.getPath(), fileMetadata.getStoredFileName());

        InputStream inputStream = storageService.download(objectKey);

        return new FileDownloadResult(fileMetadata.getFileName(), inputStream, fileMetadata.getSize(),
                fileMetadata.getContentType());
    }

    @Override
    @Transactional(readOnly = true)
    public FileStorageDownloadUrlResponseDTO shareDownloadUrl(Long id, Long userId) {
        logger.info("Generating share download URL | fileId={} userId={}", id, userId);

        FileMetadata fileMetadata = validateOwnership(id, userId);

        validateSignedUrlExpiration();

        String objectKey = storageService.buildObjectKey(fileMetadata.getPath(), fileMetadata.getStoredFileName());

        String contentDisposition =  buildContentDispositionHeader(
                fileMetadata.getFileName(), fileMetadata.getExtension());

        FileShareCacheEntry fileShareCacheEntry = getOrCreateShareUrl(id, objectKey, contentDisposition);

        logger.info("Share download URL generated | fileId={} userId={} expiresAt={}",
                id, userId, fileShareCacheEntry.expireAt());

        return new FileStorageDownloadUrlResponseDTO(id, fileShareCacheEntry.url(), fileShareCacheEntry.expireAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileStorageResponseDTO> findAllByTaskId(Long taskId, Long userId) {
        logger.info("Starting find all files by task | taskId={}, userId={}", taskId, userId);
        Task task = taskService.findByIdAndUserId(taskId, userId);

        List<FileMetadata> fileMetadataList = fileMetadataService.findAllByTaskId(task.getId());

        logger.info("Files retrieved successfully | taskId={}, totalFiles={}", taskId, fileMetadataList.size());
        return FileStorageMapper.toListDTO(fileMetadataList);
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        logger.info("Start deleting file | fileId={} userId={}", id, userId);
        FileMetadata fileMetadata = validateOwnership(id, userId);

        Task task = fileMetadata.getTask();

        validateTaskStatusForFileOperation(task);

        String objectKey = storageService.buildObjectKey(fileMetadata.getPath(), fileMetadata.getStoredFileName());

        logger.info("Marking file metadata as PENDING_DELETE | fileId={} objectKey={}",
                fileMetadata.getId(), objectKey);
        fileMetadataService.updateStatusForDelete(fileMetadata);

        logger.info("Deleting file from storage | objectKey={}", objectKey);
        storageService.delete(objectKey);

        logger.info("Deleting file metadata from database | fileId={}",
                fileMetadata.getId());
        fileMetadataService.delete(fileMetadata);

        logger.info("File deleted successfully | fileId={} objectKey={}",
                fileMetadata.getId(), objectKey);
    }

    private String buildPath(Long userId, Long taskId) {
        return userId + "/tasks/" + taskId + "/";
    }

    private String buildStoredFileName(String uuid, String extension){
        return uuid + "." + extension.toLowerCase();
    }

    private String prepareFileName(String uuid, String originalFileName, String mimeType) {
        String fileName = buildFileName(originalFileName,  mimeType, uuid);

        String sanitizedFileName = sanitizeFileName(fileName);

        return validateOrGenerateName(sanitizedFileName, mimeType, uuid);
    }

    private String buildFileName(String fileName, String mimeType, String uuid) {
        // Remove espaços do início/fim do nome do arquivo, ou usa string vazia se for null
        String trimmedName = (fileName != null) ? fileName.trim() : "";

        // Pega a extensão atual do arquivo, se houver (ex: "document.pdf" -> "pdf")
        String currentExtension = getExtension(trimmedName);

        // Caso 1: nome válido já fornecido, com extensão
        // - não vazio
        // - não começa com ponto (evita arquivos ocultos do tipo ".file")
        // - tem extensão
        // Retorna o nome como está
        if (!trimmedName.isBlank() && !trimmedName.startsWith(".") && !currentExtension.isBlank()) {
            return trimmedName;
        }

        // Obtém extensão a partir do MIME (ex: "application/pdf" -> ".pdf")
        String extension = getExtensionFromMimeType(mimeType);
        // Caso 2: nome válido fornecido, mas **sem extensão**
        // - adiciona a extensão deduzida do MIME
        if(!trimmedName.isBlank() && !trimmedName.startsWith(".") && currentExtension.isBlank()) {
            // Se o nome termina com ".", adiciona extensão diretamente
            // Caso contrário, concatena com ponto
            return (trimmedName.endsWith(".")) ? trimmedName + extension.replaceFirst("\\.", "") :
                    trimmedName + extension;
        }

        // Caso 3: nome inválido ou vazio
        // - gera nome padrão usando UUID + extensão do MIME
        return buildDefaultFileName(uuid, extension);
    }

    private String buildDefaultFileName(String uuid, String extension) {
        return "unnamed_file_" + uuid + extension;
    }

    private String buildContentDispositionHeader(String originalFileName, String extension) {
        String encoded = UriUtils.encode(originalFileName, StandardCharsets.UTF_8);

        String dispositionType = (FileExtension.fromExtension(extension).isInline())
                ? "inline"
                : "attachment";

        return dispositionType + "; filename=\"" + originalFileName + "\"; filename*=UTF-8''" + encoded;
    }

    private FileShareCacheEntry getOrCreateShareUrl(long id, String objectKey, String contentDisposition) {
        FileShareCacheEntry entry = fileShareUrlCache.getShareUrl(id);

        if(entry != null) {
            logger.debug("Share URL cache hit | fileId={} | url={} | expiresAt={}",
                    id, entry.url(), entry.expireAt());
            return entry;
        }

        String signedUrl = buildStorageSignedUrl(id, objectKey, contentDisposition);

        OffsetDateTime expiresAt = OffsetDateTime.now().plus(signedUrlExpiration);

        entry = new FileShareCacheEntry(signedUrl, expiresAt);

        fileShareUrlCache.storeShareUrl(id, entry);

        logger.debug("Storing share URL in cache | fileId={} | expiresAt={}", id, expiresAt);

        return entry;
    }

    private String buildStorageSignedUrl(long id, String objectKey, String contentDisposition) {
        boolean isLocalStorage = "local".equalsIgnoreCase(storageType);
        if (isLocalStorage) {
            String url = localSignedFileService
                    .map(service ->
                            service.generateSignedUrl(objectKey, signedUrlExpiration, contentDisposition))
                    .orElseThrow(() ->
                            new StorageException("Local storage selected but LocalSignedFileService not available",
                                    HttpStatus.INTERNAL_SERVER_ERROR.value()));

            logger.debug("Generated LOCAL share URL | fileId={}", id);
            return url;
        }

        logger.debug("Share URL cache miss - generating new signed URL | fileId={}", id);
        return storageService.generateSignedUrl(objectKey, signedUrlExpiration, contentDisposition);

    }

    private String getExtensionFromMimeType(String mimeType) {
        String extension = "";

        // Verifica se o MIME type não é o genérico "application/octet-stream"
        // Esse MIME é usado quando não se sabe o tipo real do arquivo
        if (!"application/octet-stream".equals(mimeType)) {
            try {
                // Busca o objeto MimeType correspondente no repositório do Tika
                // O Tika possui um mapeamento de MIME types para extensões comuns
                MimeType type = TIKA_CONFIG.getMimeRepository().forName(mimeType);

                // Obtém a extensão associada a esse tipo MIME
                // Ex: "image/jpeg" -> ".jpg"
                // Converte para minúsculas para padronização
                extension = type.getExtension().toLowerCase();
            }catch(MimeTypeException e) {
                // Esse catch é acionado se o MIME fornecido não existir no repositório do Tika
                // Logamos o erro, mas continuamos, porque podemos usar um fallback
                logger.warn("Invalid mime type: {}", mimeType);
            }
        }

        // Se não encontrou nenhuma extensão (string vazia ou nula)
        // usamos uma extensão padrão segura ".bin", que indica arquivo binário genérico
        if(extension.isBlank()) {
            extension = ".bin";
            logger.debug("MIME type '{}' could not be mapped to an extension, using fallback '{}'", mimeType, extension);
        }
        return extension;
    }

    private String getExtension(String fileName) {
        // Retorna a extensão do arquivo (parte após o último ponto)
        // Ex: "documento.pdf" -> "pdf", "arquivo.tar.gz" -> "gz", "arquivo" -> ""
        // Funciona mesmo se o fileName incluir caminho
        return FilenameUtils.getExtension(fileName);
    }

    private FileMetadata validateOwnership(Long id, Long userId) {
        FileMetadata fileMetadata = fileMetadataService.findById(id);

        if(!fileMetadata.getTask().getUser().getId().equals(userId)) {
            StorageLogger.logAndThrow(
                    Level.DEBUG,
                    "Ownership validation failed | fileId={} userId={}",
                    "You do not have permission to access it",
                    HttpStatus.FORBIDDEN.value(),
                    id, userId);
        }

        return fileMetadata;
    }

    private void validateFileNameNotBlank(String newFileName) {
        if(newFileName == null || newFileName.isBlank()) {
            StorageLogger.logAndThrow(
                    Level.DEBUG,
                    "Invalid rename request. New name is null or blank",
                    "Invalid file name: new name cannot be null or blank",
                    HttpStatus.BAD_REQUEST.value());
        }
    }

    private String sanitizeFileName(String fileName) {
        // normaliza unicode (corrige ç, á, etc)
        fileName = Normalizer.normalize(fileName, Normalizer.Form.NFC);

        // remove path traversal ".."
        fileName = fileName.replaceAll("\\.{2,}", "_");

        // remove pontos no início do nome (evita arquivos ocultos)
        fileName = fileName.replaceAll("^\\.+", "");

        // remove quebra de linha (CRLF injection)
        fileName = fileName.replaceAll("[\\r\\n]", "");

        // permite unicode + espaço + parênteses + emoji
        fileName = fileName.replaceAll("[^\\p{L}\\p{N}\\p{M}\\p{So}._\\- ()]", "_");

        // normaliza espaços
        fileName = fileName.replaceAll(" +", " ");

        // remove espaços antes de pontos e limpa as pontas
        // Ex: "foto .jpg" -> "foto.jpg"
        fileName = fileName.replaceAll(" \\.", ".");

        return fileName.trim();
    }

    private void validateSanitizedFileName(String newFileName) {
        // Rejeita nomes vazios OU compostos apenas por caracteres não significativos.
        // Após a sanitização, entradas como "/.", "///", "...", "---" podem virar "_", ".", "-", ou espaços.
        // Esta validação garante que o nome contenha pelo menos um caractere significativo
        // (letra ou número), evitando nomes como "_", "...", "---" ou "   ".
        if(newFileName.isBlank() || newFileName.matches("[._\\- ]+")) {
            StorageLogger.logAndThrow(
                    Level.DEBUG,
                    "Invalid rename request. New file name contains only invalid characters",
                    "Invalid file name: must contain at least one letter or number",
                    HttpStatus.BAD_REQUEST.value());
        }
    }

    private String validateOrGenerateName(String fileName, String mimeType, String uuid) {
        String fileNameWithoutExtension = FilenameUtils.getBaseName(fileName);
        String extension = getExtensionFromMimeType(mimeType);

        if(fileNameWithoutExtension.isBlank() || fileNameWithoutExtension.matches("[._\\- ]+")) {
            logger.debug("File name '{}' is invalid after sanitization, generating default name with UUID '{}{}'",
                    fileNameWithoutExtension, uuid, extension);

            return buildDefaultFileName(uuid, extension);
        }

        return fileName;
    }

    private void validateTaskStatusForFileOperation(Task task) {
        if (TaskStatus.CANCELLED.equals(task.getStatus()) || TaskStatus.DONE.equals(task.getStatus()))  {
            logger.debug("Task with status DONE or CANCELLED cannot modify files | taskId={} status={}",
                    task.getId(), task.getStatus());
            throw new TaskStatusNotAllowedException("Task with status DONE or CANCELLED cannot modify files");
        }
    }

    private void validateUploadRequest(MultipartFile file, Long taskId, Long userId) {
        if (file == null) {
            StorageLogger.logAndThrow(
                    Level.DEBUG,
                    "Upload failed: file is null | userId={} taskId={}",
                    "File must not be null",
                    HttpStatus.BAD_REQUEST.value(),
                    userId, taskId);
        }
        if (file.isEmpty()) {
            StorageLogger.logAndThrow(
                    Level.DEBUG,
                    "Upload failed: file is empty | userId={} taskId={}",
                    "File must not be empty",
                    HttpStatus.BAD_REQUEST.value(),
                    userId, taskId);
        }
        if (taskId == null || userId == null) {
            StorageLogger.logAndThrow(
                    Level.DEBUG,
                    "Upload failed: invalid identifiers | userId={} taskId={}",
                    "TaskId and userId must not be null",
                    HttpStatus.BAD_REQUEST.value(),
                    userId, taskId);
        }
    }

    private String detectMimeType(MultipartFile file) {
        // TikaInputStream é uma implementação especial de InputStream usada pelo Apache Tika.
        // Diferenças importantes:
        // 1. Pode fornecer acesso ao arquivo ou stream subjacente várias vezes sem precisar recarregar tudo.
        // 2. Suporta otimizações internas do Tika para detecção de MIME, como mapeamento de magic bytes.
        // 3. Trabalha melhor com arquivos grandes ou quando o Tika precisa "marcar/resetar" o stream.
        //
        // Usamos TikaInputStream em vez de InputStream simples para garantir maior precisão
        // e robustez na detecção do tipo MIME.
        try(TikaInputStream inputStream = TikaInputStream.get(file.getInputStream())){
            // Usa o Tika para detectar o tipo MIME real do arquivo
            // - inputStream: conteúdo do arquivo
            // - file.getOriginalFilename(): nome original do arquivo (ajuda o Tika a deduzir o tipo)
            String mimeType = tika.detect(inputStream, file.getOriginalFilename());
            // Se o Tika não conseguir detectar o tipo, usamos um fallback seguro: "application/octet-stream"
            // Esse é o MIME genérico para arquivos binários desconhecidos
            return (mimeType != null) ? mimeType : "application/octet-stream";
        } catch (IOException e) {
            throw new StorageException("Failed to upload file: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    private void validateAllowedExtension(String extension) {
        if (!FileExtension.isAllowedExtension(extension.toLowerCase()))
            StorageLogger.logAndThrow(
                    Level.DEBUG,
                    "Attempt to upload a file with an unauthorized extension: {}",
                    "File extension not allowed: " + extension.toLowerCase(),
                    HttpStatus.BAD_REQUEST.value(),
                    extension.toLowerCase());
    }

    private void validateExtensionMatchesContent(String mimeType, String extension) throws MimeTypeException {
        // Converte a string MIME (ex: "application/pdf") em um objeto MediaType
        // Isso permite separar tipo e subtipo, além de usar métodos convenientes do MediaType.
        MediaType mediaType = MediaType.parse(mimeType);

        // Se a conversão para MediaType foi bem-sucedida, pegamos apenas o "base type" (tipo/subtipo)
        // Ex: "application/pdf" -> "application/pdf", mas descarta parâmetros extras se houver
        // Caso MediaType seja null, usamos o mimeType original
        String baseMimeType = (mediaType != null) ? mediaType.getBaseType().toString() : mimeType;

        // Usa o repositório de MIME types do Tika para obter o objeto MimeType correspondente
        // O objeto MimeType do Tika contém informações como: nome, descrição e extensões válidas
        MimeType type = TIKA_CONFIG.getMimeRepository().forName(baseMimeType);

        // Lista todas as extensões válidas associadas a esse tipo de conteúdo
        // - remove o ponto inicial (ex: ".pdf" -> "pdf")
        // - converte para lowercase para evitar problemas de case
        List<String> validExtensionsForContent = type.getExtensions().stream()
                .map(ext -> ext.replace(".", "").toLowerCase())
                .toList();

        // Verifica se a extensão fornecida pelo usuário está entre as extensões válidas para o conteúdo detectado
        // Se não estiver, significa que o arquivo pode ter sido renomeado de forma incorreta ou maliciosa
        if (!validExtensionsForContent.contains(extension.toLowerCase()))
            StorageLogger.logAndThrow(
                    Level.DEBUG,
                    "Security Alert: Extension '.{}' does not match real content '{}'",
                    "File content does not match the provided extension.",
                    HttpStatus.BAD_REQUEST.value(),
                    extension, mimeType);
    }

    private void validateSignedUrlExpiration() {
        if(signedUrlExpiration == null || signedUrlExpiration.isZero() || signedUrlExpiration.isNegative()) {
            StorageLogger.logAndThrow(
                    Level.DEBUG,
                    "Invalid signed URL expiration duration | duration={}",
                    "Signed URL expiration must be greater than zero",
                    HttpStatus.BAD_REQUEST.value(),
                    signedUrlExpiration
            );
        }
    }

    private FileStorageResponseDTO handleFileUpload(
            String validFileName, String path, String storedFileName, String extension, String mimeType,
            Task task, InputStream inputStream, long contentLength, String objectKey
    ) {
        FileMetadata fileMetadata = persistFileMetadata(validFileName, path, storedFileName, extension,
                mimeType, contentLength, task);

        uploadToStorage(mimeType, inputStream, contentLength, objectKey);

        reconcileAndActivate(fileMetadata);

        logger.info("File uploaded successfully | storedFileName={}, path={}", storedFileName, path);
        return FileStorageMapper.toDTO(fileMetadata);
    }

    private void uploadToStorage(String mimeType, InputStream inputStream, long contentLength,
                                 String objectKey) {
        Path tempFile = null;
        try {
            // Cria arquivo temporário com UUID para evitar colisões
            tempFile = Files.createTempFile("upload_", ".tmp");

            // Copia o conteúdo do InputStream para o arquivo
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

            // Delegação para o storage (S3, Azure, etc.)
            storageService.upload(tempFile, contentLength, objectKey, mimeType);

            logger.info("File uploaded to storage | objectKey={}", objectKey);
        } catch (IOException e) {
            // Erro de I/O ao criar/copiar/deletar arquivo temporário
            StorageLogger.logAndThrow(
                    Level.ERROR,
                    "I/O error while preparing temp file for upload | objectKey={}, mimeType={}",
                    "Error preparing file for storage upload",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    e,
                    objectKey, mimeType);
        }finally {
            if (tempFile != null) {
                try {
                    // Remove o arquivo temporário do disco
                    // evita vazamento de arquivos no temp
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    // Não lança exceção aqui para não sobrescrever erro original
                    // Apenas loga warning
                    logger.warn("Failed to delete temp file {}", tempFile, e);
                }
            }
        }
    }

    private FileMetadata persistFileMetadata(String sanitizedFileName, String path, String storedFileName,
                                             String extension, String mimeType, long size, Task task) {
        return fileMetadataService.createMetadata(sanitizedFileName, path, storedFileName,
                extension, mimeType, size, task);
    }

    private void reconcileAndActivate(FileMetadata fileMetadata) {
        if(fileMetadata == null) {
            StorageLogger.logAndThrow(
                    Level.DEBUG,
                    "File metadata is null during reconcile",
                    "File metadata is null",
                    HttpStatus.BAD_REQUEST.value());
        }

        String objectKey = storageService.buildObjectKey(fileMetadata.getPath(), fileMetadata.getStoredFileName());
        boolean exists = storageService.exists(objectKey);

        if(!exists) {
            logger.info("Storage file missing during reconcile | objectKey={}", objectKey);
            fileMetadataService.delete(fileMetadata);
            logger.info("Deleted file metadata due to missing storage | id={}, fileName={}", fileMetadata.getId(), fileMetadata.getFileName());
            throw new StorageException("File does not exist", HttpStatus.BAD_REQUEST.value());
        }
        logger.info("Storage file exists | objectKey={}", objectKey);
        fileMetadataService.updateStatusForActive(fileMetadata);
        logger.info("File metadata set to ACTIVE | id={}, fileName={}", fileMetadata.getId(), fileMetadata.getFileName());
    }

    private void validateFileSize(long size) {
        // Verifica se o tamanho do arquivo enviado excede o limite configurado
        // - size: tamanho do arquivo em bytes
        // - maxFileSize: limite máximo definido nas propriedades (ex: 20MB)
        // Se o tamanho for maior que o permitido, lança exceção e interrompe o upload
        if(size > maxFileSize.toBytes())
            StorageLogger.logAndThrow(
                    Level.DEBUG,
                    "File size exceeds the maximum allowed limit | size={} bytes, maxAllowed={} bytes",
                    "File size exceeds limit",
                    HttpStatus.BAD_REQUEST.value(),
                    size, maxFileSize.toBytes());
    }

    private void validateMaxFilesLimit(Long taskId) {
        if(fileMetadataService.hasReachedMaxFiles(taskId))
            StorageLogger.logAndThrow(
                    Level.DEBUG,
                    "Maximum number of files reached for this task. | taskId={}",
                    "Maximum number of files reached for this task.",
                    HttpStatus.BAD_REQUEST.value(),
                    taskId);
    }

    private void validateTotalSizeLimit(Long taskId, long size) {
        if(fileMetadataService.hasReachedMaxTotalSize(taskId, size))
           StorageLogger.logAndThrow(
                   Level.DEBUG,
                   "Total file size limit exceeded for taskId={} | newFileSize={} bytes",
                    "Total size of files for this task exceeds the maximum allowed limit.",
                    HttpStatus.BAD_REQUEST.value(),
                    taskId, size);
    }
}
