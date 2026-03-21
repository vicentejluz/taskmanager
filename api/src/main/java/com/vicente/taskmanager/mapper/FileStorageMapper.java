package com.vicente.taskmanager.mapper;

import com.vicente.taskmanager.domain.entity.FileMetadata;
import com.vicente.taskmanager.dto.response.FileStorageResponseDTO;
import java.util.List;

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

    public static List<FileStorageResponseDTO> toListDTO(List<FileMetadata> fileMetadataList) {
        return fileMetadataList.stream().map(FileStorageMapper::toDTO).toList();
    }
}
