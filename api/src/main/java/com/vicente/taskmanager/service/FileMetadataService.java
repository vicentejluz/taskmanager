package com.vicente.taskmanager.service;

import com.vicente.taskmanager.domain.entity.FileMetadata;
import com.vicente.taskmanager.domain.entity.Task;

import java.util.List;

public interface FileMetadataService {
    FileMetadata createMetadata(String fileName, String path, String storedFileName, String extension,
                                String contentType, Long size, Task task);
    String update(Long id, String newFileName);
    void delete(FileMetadata fileMetadata);
    FileMetadata findById(Long id);
    FileMetadata findByStoredFileName(String storedFileName);
    List<FileMetadata> findAllByTaskId(Long taskId);
    void updateStatusForActive(FileMetadata fileMetadata);
    void updateStatusForDelete(FileMetadata fileMetadata);
    boolean hasReachedMaxFiles(Long taskId);
    boolean hasReachedMaxTotalSize(Long taskId, long size);

}
