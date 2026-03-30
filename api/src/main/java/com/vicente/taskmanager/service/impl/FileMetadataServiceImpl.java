package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.domain.entity.FileMetadata;
import com.vicente.taskmanager.domain.entity.Task;
import com.vicente.taskmanager.domain.enums.FileMetadataStatus;
import com.vicente.taskmanager.exception.FileMetadataNotFoundException;
import com.vicente.taskmanager.repository.FileMetadataRepository;
import com.vicente.taskmanager.service.FileMetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;

import java.util.List;

@Service
public class FileMetadataServiceImpl implements FileMetadataService {
    private final FileMetadataRepository fileMetadataRepository;
    private final Integer maxFilesPerTask;
    private final DataSize maxTotalSize;
    private static final Logger logger = LoggerFactory.getLogger(FileMetadataServiceImpl.class);

    public FileMetadataServiceImpl(FileMetadataRepository fileMetadataRepository,
                                   @Value("${file.upload.max-files-per-task}") Integer maxFilesPerTask,
                                   @Value("${file.upload.max-total-size}") DataSize maxTotalSize) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.maxFilesPerTask = maxFilesPerTask;
        this.maxTotalSize = maxTotalSize;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FileMetadata createMetadata(String fileName, String path, String storedFileName, String extension,
                                                 String contentType, Long size, Task task) {
        logger.info("Creating file metadata | taskId={}, fileName={}, size={}", task.getId(), fileName, size);

        FileMetadata fileMetadata = new FileMetadata(fileName, path, extension,
                storedFileName, contentType, size, task);

        fileMetadataRepository.saveAndFlush(fileMetadata);
        logger.info("File metadata has been saved | id={}, fileName={}", fileMetadata.getId(), fileName);
        return fileMetadata;
    }

    @Override
    @Transactional
    public String update(Long id, String newFileName) {
        logger.info("Updating file metadata | id={}, newFileName={}", id, newFileName);
        FileMetadata fileMetadata = getFileMetadataOrThrow(id);

        newFileName = buildFinalFileName(newFileName, fileMetadata);

        if(!newFileName.equals(fileMetadata.getFileName())) {
            String oldFileName = fileMetadata.getFileName();
            fileMetadata.setFileName(newFileName);
            fileMetadataRepository.save(fileMetadata);
            logger.info("Updated file metadata | id={}, oldFileName={}, newFileName={}",
                    id, oldFileName, newFileName);
        }

        return newFileName;
    }

    @Override
    @Transactional
    public void delete(FileMetadata fileMetadata) {
        logger.info("Deleting file metadata | id={}", fileMetadata.getId());
        fileMetadataRepository.delete(fileMetadata);
        logger.info("File metadata has been deleted");
    }

    @Override
    @Transactional(readOnly = true)
    public FileMetadata findById(Long id) {
        logger.info("Finding file metadata | id={}", id);
        return getFileMetadataOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileMetadata> findAllByTaskId(Long taskId) {
        logger.info("Finding file metadata by taskId | taskId={}", taskId);
        return fileMetadataRepository.findAllByTaskId(taskId);
    }

    @Override
    @Transactional
    public void updateStatusForActive(FileMetadata fileMetadata) {
        logger.info("Updating file metadata status for Active | id={}", fileMetadata.getId());
        fileMetadata.setStatus(FileMetadataStatus.ACTIVE);

        fileMetadataRepository.save(fileMetadata);
        logger.info("File metadata status has been updated | id={}", fileMetadata.getId());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatusForDelete(FileMetadata fileMetadata) {
        logger.info("Updating file metadata status for pending delete | id={}", fileMetadata.getId());
        fileMetadata.setStatus(FileMetadataStatus.PENDING_DELETE);
        fileMetadataRepository.save(fileMetadata);
        logger.info("File metadata status for pending delete  has been updated | id={}", fileMetadata.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasReachedMaxFiles(Long taskId) {
        return fileMetadataRepository.countAllByTaskId(taskId) >= maxFilesPerTask;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasReachedMaxTotalSize(Long taskId, long size) {
        long currentTotalSize = fileMetadataRepository.sumAllByTaskId(taskId);
        long newTotalSize = currentTotalSize + size;

        logger.debug("Checking total file size limit | taskId={} | currentTotal={} bytes | newFileSize={} bytes | " +
                        "newTotal={} bytes | maxAllowed={} bytes",
                taskId, currentTotalSize, size, newTotalSize, maxTotalSize.toBytes());

        return newTotalSize > maxTotalSize.toBytes();
    }


    private FileMetadata getFileMetadataOrThrow(Long id) {
        return fileMetadataRepository.findById(id).orElseThrow(() -> {
            logger.debug("File metadata not found | id={}", id);
            return new FileMetadataNotFoundException("File metadata not found");
        });
    }

    private String buildFinalFileName(String newFileName, FileMetadata fileMetadata) {
        return (newFileName.endsWith("."))
                ? newFileName.concat(fileMetadata.getExtension())
                : String.format("%s.%s", newFileName, fileMetadata.getExtension());
    }
}
