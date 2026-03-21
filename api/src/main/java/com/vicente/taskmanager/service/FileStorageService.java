package com.vicente.taskmanager.service;

import com.vicente.taskmanager.dto.internal.FileDownloadResult;
import com.vicente.taskmanager.dto.response.FileStorageResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface FileStorageService {
    FileStorageResponseDTO upload(MultipartFile file, Long taskId, Long userId);
    FileDownloadResult download(UUID id, Long userId);
    List<FileStorageResponseDTO> findAllByTaskId(Long taskId, Long userId);
    void delete(UUID id, Long userId);
}
