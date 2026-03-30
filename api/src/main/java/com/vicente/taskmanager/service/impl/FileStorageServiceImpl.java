package com.vicente.taskmanager.service.impl;

import com.vicente.storage.StorageService;
import com.vicente.storage.exception.StorageException;
import com.vicente.storage.util.StorageLogger;
import com.vicente.taskmanager.domain.entity.FileMetadata;
import com.vicente.taskmanager.domain.entity.Task;
import com.vicente.taskmanager.domain.enums.FileExtension;
import com.vicente.taskmanager.domain.enums.TaskStatus;
import com.vicente.taskmanager.dto.internal.FileDownloadResult;
import com.vicente.taskmanager.dto.response.FileStorageRenameResponseDTO;
import com.vicente.taskmanager.dto.response.FileStorageResponseDTO;
import com.vicente.taskmanager.exception.TaskStatusNotAllowedException;
import com.vicente.taskmanager.mapper.FileStorageMapper;
import com.vicente.taskmanager.service.FileMetadataService;
import com.vicente.taskmanager.service.FileStorageService;
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

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {
    private final StorageService storageService;
    private final FileMetadataService fileMetadataService;
    private final TaskService taskService;
    private final Tika tika;
    private final DataSize maxFileSize;
    private static final TikaConfig TIKA_CONFIG = TikaConfig.getDefaultConfig();
    private static final Logger logger = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    public FileStorageServiceImpl(StorageService storageService, FileMetadataService fileMetadataService,
                                  TaskService taskService, @Value("${file.upload.max-size}") DataSize maxFileSize) {
        this.storageService = storageService;
        this.fileMetadataService = fileMetadataService;
        this.taskService = taskService;
        this.maxFileSize = maxFileSize;
        this.tika = new Tika();
    }

    @Override
    public FileStorageResponseDTO upload(MultipartFile file, Long taskId, Long userId) {
        validateUploadRequest(file, taskId, userId);

        long size = file.getSize();

        validateFileSize(size);

        validateMaxFilesLimit(taskId);

        validateTotalSizeLimit(taskId, size);

        Task task = taskService.findByIdAndUserId(taskId, userId);

        validateTaskStatusForFileOperation(task);

        String mimeType = detectMimeType(file);

        try(InputStream inputStream = file.getInputStream()) {

            logger.info("Start uploading file | taskId={}, userId={}, originalFileName={}, size={}",
                    taskId, userId, file.getOriginalFilename(), file.getSize());

            String uuid = UUID.randomUUID().toString();

            String fileName = buildFileName(file.getOriginalFilename(),  mimeType, uuid);

            String sanitizedFileName = sanitizeFileName(fileName);

            String extension = getExtension(fileName);

            String path = buildPath(userId, taskId);

            validateAllowedExtension(extension);

            validateExtensionMatchesContent(mimeType, extension);

            String storedFileName = buildStoredFileName(uuid, extension);

            String objectKey = buildObjectKey(path, storedFileName);

            return handleFileUpload(sanitizedFileName, path, storedFileName, extension, mimeType, size, task,
                    inputStream, objectKey);
        } catch (IOException e) {
            throw new StorageException("Failed to upload file: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        } catch (MimeTypeException e) {
            throw new StorageException("Error validating file media type and consistency." + e.getMessage(),
                    HttpStatus.BAD_REQUEST.value(), e);
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

        logger.info("File renamed successfully | id={}, newFileName={}", id, newFileName);

        return new FileStorageRenameResponseDTO(id, newFileName);
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownloadResult download(Long id, Long userId) {
        logger.info("Start downloading file | id={}", id);
        FileMetadata fileMetadata = validateOwnership(id, userId);

        String objectKey = buildObjectKey(fileMetadata.getPath(), fileMetadata.getStoredFileName());

        InputStream inputStream = storageService.download(objectKey);

        return new FileDownloadResult(fileMetadata.getFileName(), inputStream, fileMetadata.getSize(),
                fileMetadata.getContentType());
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
    public void delete(Long id, Long userId) {
        logger.info("Start deleting file | fileId={} userId={}", id, userId);
        FileMetadata fileMetadata = validateOwnership(id, userId);

        Task task = fileMetadata.getTask();

        validateTaskStatusForFileOperation(task);

        String objectKey = buildObjectKey(fileMetadata.getPath(), fileMetadata.getStoredFileName());

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

    private String buildObjectKey(String path, String storedFileName) {
        return path + storedFileName;
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
        return "unnamed_file_" + uuid  + extension;
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
            logAndThrow(
                    "Ownership validation failed | fileId={} userId={}",
                    "You do not have permission to access it",
                    HttpStatus.FORBIDDEN.value(),
                    id, userId);
        }

        return fileMetadata;
    }

    private void validateFileNameNotBlank(String newFileName) {
        if(newFileName == null || newFileName.isBlank()) {
            logAndThrow("Invalid rename request. New name is null or blank",
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
            logAndThrow("Invalid rename request. New file name contains only invalid characters",
                    "Invalid file name: must contain at least one letter or number",
                    HttpStatus.BAD_REQUEST.value());
        }
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
            logAndThrow("Upload failed: file is null | userId={} taskId={}",
                    "File must not be null",
                    HttpStatus.BAD_REQUEST.value(),
                    userId, taskId);
        }
        if (file.isEmpty()) {
            logAndThrow("Upload failed: file is empty | userId={} taskId={}",
                    "File must not be empty",
                    HttpStatus.BAD_REQUEST.value(),
                    userId, taskId);
        }
        if (taskId == null || userId == null) {
            logAndThrow("Upload failed: invalid identifiers | userId={} taskId={}",
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
            logAndThrow("Attempt to upload a file with an unauthorized extension: {}",
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
            logAndThrow("Security Alert: Extension '.{}' does not match real content '{}'",
                    "File content does not match the provided extension.",
                    HttpStatus.BAD_REQUEST.value(),
                    extension, mimeType);
    }

    private FileStorageResponseDTO handleFileUpload(
            String sanitizedFileName, String path, String storedFileName, String extension, String mimeType,
            long size, Task task, InputStream inputStream, String objectKey
    ) {
        FileMetadata fileMetadata = persistFileMetadata(sanitizedFileName, path, storedFileName, extension,
                mimeType, size, task);

        uploadToStorage(mimeType, size, inputStream, objectKey);

        reconcileAndActivate(fileMetadata);

        logger.info("File uploaded successfully | storedFileName={}, path={}", storedFileName, path);
        return FileStorageMapper.toDTO(fileMetadata);
    }

    private void uploadToStorage(String mimeType, long size, InputStream inputStream, String objectKey) {
        storageService.upload(inputStream, size, objectKey, mimeType);
        logger.info("File uploaded to storage | objectKey={}, size={}", objectKey, size);
    }

    private FileMetadata persistFileMetadata(String sanitizedFileName, String path, String storedFileName,
                                             String extension, String mimeType, long size, Task task) {
        return fileMetadataService.createMetadata(sanitizedFileName, path, storedFileName,
                extension, mimeType, size, task);
    }

    private void reconcileAndActivate(FileMetadata fileMetadata) {
        if(fileMetadata == null) {
            logAndThrow("File metadata is null during reconcile",
                    "File metadata is null",
                    HttpStatus.BAD_REQUEST.value());
        }

        String objectKey = buildObjectKey(fileMetadata.getPath(), fileMetadata.getStoredFileName());
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
            logAndThrow("File size exceeds the maximum allowed limit | size={} bytes, maxAllowed={} bytes",
                    "File size exceeds limit",
                    HttpStatus.BAD_REQUEST.value(),
                    size, maxFileSize.toBytes());
    }

    private void validateMaxFilesLimit(Long taskId) {
        if(fileMetadataService.hasReachedMaxFiles(taskId))
            logAndThrow("Maximum number of files reached for this task. | taskId={}",
                    "Maximum number of files reached for this task.",
                    HttpStatus.BAD_REQUEST.value(),
                    taskId);
    }

    private void validateTotalSizeLimit(Long taskId, long size) {
        if(fileMetadataService.hasReachedMaxTotalSize(taskId, size))
            logAndThrow("Total file size limit exceeded for taskId={} | newFileSize={} bytes",
                    "Total size of files for this task exceeds the maximum allowed limit.",
                    HttpStatus.BAD_REQUEST.value(),
                    taskId, size);
    }

    private void logAndThrow(String msgLog, String msgThrow, int statusCode, Object... args) {
        StorageLogger.logAndThrow(Level.DEBUG, msgLog, msgThrow, statusCode, args);
    }
}
