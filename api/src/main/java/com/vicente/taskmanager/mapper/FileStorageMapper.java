package com.vicente.taskmanager.mapper;

import com.vicente.taskmanager.domain.entity.FileMetadata;
import com.vicente.taskmanager.dto.response.FileStorageResponseDTO;

public class FileStorageMapper {

    public static FileStorageResponseDTO toDTO(FileMetadata fileMetadata) {
        return  new FileStorageResponseDTO(
                fileMetadata.getTask().getId(),
                fileMetadata.getId(),
                fileMetadata.getFileName(),
                fileMetadata.getContentType(),
                fileMetadata.getSize(),
                fileMetadata.getCreatedAt()
        );
    }
}
