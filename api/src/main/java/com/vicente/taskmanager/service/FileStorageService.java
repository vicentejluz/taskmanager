package com.vicente.taskmanager.service;

import com.vicente.taskmanager.dto.internal.FileDownloadResult;
import com.vicente.taskmanager.dto.response.FileStorageDownloadUrlResponseDTO;
import com.vicente.taskmanager.dto.response.FileStorageRenameResponseDTO;
import com.vicente.taskmanager.dto.response.FileStorageResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {
    FileStorageResponseDTO upload(MultipartFile file, Long taskId, Long userId);
    FileStorageRenameResponseDTO rename(Long id, Long userId, String newFileName);
    FileDownloadResult download(Long id, Long userId);
    FileStorageDownloadUrlResponseDTO shareDownloadUrl(Long id, Long userId);
    List<FileStorageResponseDTO> findAllByTaskId(Long taskId, Long userId);
    void delete(Long id, Long userId);
}
