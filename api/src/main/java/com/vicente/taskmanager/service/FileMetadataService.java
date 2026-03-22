package com.vicente.taskmanager.service;

import com.vicente.taskmanager.domain.entity.FileMetadata;
import com.vicente.taskmanager.domain.entity.Task;

import java.util.List;

public interface FileMetadataService {
    FileMetadata createMetadata(String fileName, String path, String storedFileName, String extension,
                                String contentType, Long size, Task task);
    void delete(FileMetadata fileMetadata);
    FileMetadata findById(Long id);
    List<FileMetadata> findAllByTaskId(Long taskId);


}
