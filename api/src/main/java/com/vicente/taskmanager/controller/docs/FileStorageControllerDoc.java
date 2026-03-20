package com.vicente.taskmanager.controller.docs;

import com.vicente.taskmanager.domain.entity.User;
import com.vicente.taskmanager.dto.response.FileStorageResponseDTO;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Tag(name = "File Storage", description = "File storage management endpoints")
@SecurityRequirement(name = "bearerAuth")
public interface FileStorageControllerDoc {
    ResponseEntity<FileStorageResponseDTO> upload(MultipartFile file, Long taskId, User user);

    ResponseEntity<Void> delete(UUID id, User user);
}
