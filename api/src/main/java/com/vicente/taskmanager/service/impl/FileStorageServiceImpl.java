package com.vicente.taskmanager.service.impl;

import com.vicente.storage.StorageService;
import com.vicente.storage.exception.StorageException;
import com.vicente.taskmanager.domain.entity.FileMetadata;
import com.vicente.taskmanager.domain.entity.Task;
import com.vicente.taskmanager.dto.internal.FileDownloadResult;
import com.vicente.taskmanager.dto.response.FileStorageResponseDTO;
import com.vicente.taskmanager.mapper.FileStorageMapper;
import com.vicente.taskmanager.service.FileMetadataService;
import com.vicente.taskmanager.service.FileStorageService;
import com.vicente.taskmanager.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            ".pdf", ".docx", ".doc", ".xlsx", ".xls", ".pptx", ".ppt", ".txt", ".zip", ".jpg", ".jpeg",
            ".png", ".7zip");
    private static final Logger logger = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    public FileStorageServiceImpl(StorageService storageService, FileMetadataService fileMetadataService,
                                  TaskService taskService) {
        this.storageService = storageService;
        this.fileMetadataService = fileMetadataService;
        this.taskService = taskService;
    }


    @Override
    @Transactional
    public FileStorageResponseDTO upload(MultipartFile file, Long taskId, Long userId) {
        if (file == null) {
            logger.debug("Upload failed: file is null | userId={} taskId={}", userId, taskId);
            throw new StorageException("File must not be null", HttpStatus.BAD_REQUEST.value());
        }
        if (file.isEmpty()) {
            logger.debug("Upload failed: file is empty | userId={} taskId={}", userId, taskId);
            throw new StorageException("File must not be empty", HttpStatus.BAD_REQUEST.value());
        }
        if (taskId == null || userId == null) {
            logger.debug("Upload failed: invalid identifiers | userId={} taskId={}", userId, taskId);
            throw new StorageException("TaskId and userId must not be null", HttpStatus.BAD_REQUEST.value());
        }

        try(InputStream inputStream = file.getInputStream()) {
            String mimeType = file.getContentType();
            long size = file.getSize();

            logger.info("Start uploading file | taskId={}, userId={}, originalFileName={}, size={}",
                    taskId, userId, file.getOriginalFilename(), file.getSize());

            Task task = taskService.findByIdAndUserId(taskId, userId);

            String uuid = UUID.randomUUID().toString();

            String fileName = buildFileName(file.getOriginalFilename(),  mimeType, uuid);

            String sanitizedFileName = sanitizeFileName(fileName);

            String extension = getExtension(fileName);

            if(extension == null) {
                extension = ".bin";
                sanitizedFileName += extension;
                logger.debug("File has no extension, fallback applied: {}", sanitizedFileName);
            }else if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
                logger.debug("Attempt to upload a file with an unauthorized extension: {}", extension.toLowerCase());
                throw new StorageException("File extension not allowed: " + extension.toLowerCase(),
                        HttpStatus.BAD_REQUEST.value());
            }

            String path = buildPath(userId, taskId);

            String storedFileName = buildStoredFileName(uuid, extension);

            String objectKey = buildObjectKey(path, storedFileName);

            if(mimeType == null) {
                mimeType = "application/octet-stream";
            }

            FileMetadata fileMetadata =
                    fileMetadataService.createMetadata(sanitizedFileName, path, storedFileName,
                    extension, mimeType, size, task);

            storageService.upload(inputStream, size, objectKey, mimeType);

            logger.info("File uploaded successfully | storedFileName={}, path={}", storedFileName, path);

            return FileStorageMapper.toDTO(fileMetadata);

        } catch (IOException e) {
            throw new StorageException("Failed to upload file: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownloadResult download(UUID id, Long userId) {
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
    @Transactional
    public void delete(UUID id, Long userId) {
        logger.info("Start deleting file with id={}", id);
        FileMetadata fileMetadata = validateOwnership(id, userId);

        String objectKey = buildObjectKey(fileMetadata.getPath(), fileMetadata.getStoredFileName());

        fileMetadataService.delete(fileMetadata);

        storageService.delete(objectKey);

        logger.info("File deleted successfully");
    }

    private String buildPath(Long userId, Long taskId) {
        return userId + "/tasks/" + taskId + "/";
    }

    private String buildStoredFileName(String uuid, String extension){
        return uuid + extension.toLowerCase();
    }

    private String buildObjectKey(String path, String storedFileName) {
        return path + storedFileName;
    }

    private String buildFileName(String fileName, String mimeType, String uuid) {
        if(fileName != null && !fileName.isBlank())
            return fileName;
        if(mimeType != null)
            return "unnamed_file_" + uuid + "." +
                    mimeType.substring(mimeType.lastIndexOf('/') + 1);

        return "unnamed_file_" + uuid;
    }

    private String getExtension(String fileName) {
        if(fileName.lastIndexOf(".") == -1)
            return null;
        return fileName.substring(fileName.lastIndexOf("."));
    }

    private FileMetadata validateOwnership(UUID id, Long userId) {
        FileMetadata fileMetadata = fileMetadataService.findById(id);

        if(!fileMetadata.getTask().getUser().getId().equals(userId)) {
            throw new StorageException("You do not have permission to access it",  HttpStatus.FORBIDDEN.value());
        }

        return fileMetadata;
    }

    private String sanitizeFileName(String fileName) {
        // normaliza unicode (corrige ç, á, etc)
        fileName = Normalizer.normalize(fileName, Normalizer.Form.NFC);
        // remove quebra de linha (CRLF injection)
        fileName = fileName.replaceAll("[\\r\\n]", "");
        // permite unicode + espaço + parênteses + emoji
        fileName = fileName.replaceAll("[^\\p{L}\\p{N}\\p{M}\\p{So}._\\- ()]", "_");
        // normaliza espaços
        fileName = fileName.trim().replaceAll(" +", " ");

        return fileName.trim();
    }
}
