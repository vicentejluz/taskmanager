package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.domain.entity.FileMetadata;
import com.vicente.taskmanager.domain.entity.Task;
import com.vicente.taskmanager.exception.FileMetadataNotFoundException;
import com.vicente.taskmanager.repository.FileMetadataRepository;
import com.vicente.taskmanager.service.FileMetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FileMetaDataServiceImpl implements FileMetadataService {
    private final FileMetadataRepository fileMetadataRepository;
    private static final Logger logger = LoggerFactory.getLogger(FileMetaDataServiceImpl.class);

    public FileMetaDataServiceImpl(FileMetadataRepository fileMetadataRepository) {
        this.fileMetadataRepository = fileMetadataRepository;
    }

    @Override
    @Transactional
    public FileMetadata createMetadata(String fileName, String path, String storedFileName, String extension,
                                                 String contentType, Long size, Task task) {
        logger.info("Creating file metadata | taskId={}, fileName={}, size={}", task.getId(), fileName, size);

        FileMetadata fileMetadata = new FileMetadata(fileName, path, extension,
                storedFileName, contentType, size, task);

        logger.info("File metadata has been saved | id={}, fileName={}", fileMetadata.getId(),  fileName);

        return fileMetadataRepository.saveAndFlush(fileMetadata);
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
    public FileMetadata findById(UUID id) {
        logger.info("Finding file metadata | id={}", id);
        return fileMetadataRepository.findById(id).orElseThrow(() ->{
            logger.debug("File metadata not found | id={}", id);
            return new FileMetadataNotFoundException("File metadata not found");
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileMetadata> findAllByTaskId(Long taskId) {
        logger.info("Finding file metadata by taskId | taskId={}", taskId);
        return fileMetadataRepository.findAllByTaskId(taskId);
    }
}
