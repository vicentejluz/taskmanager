package com.vicente.taskmanager.controller;

import com.vicente.taskmanager.controller.docs.FileStorageControllerDoc;
import com.vicente.taskmanager.domain.entity.User;
import com.vicente.taskmanager.dto.internal.FileDownloadResult;
import com.vicente.taskmanager.dto.request.FileStorageRenameRequestDTO;
import com.vicente.taskmanager.dto.response.FileStorageDownloadUrlResponseDTO;
import com.vicente.taskmanager.dto.response.FileStorageRenameResponseDTO;
import com.vicente.taskmanager.dto.response.FileStorageResponseDTO;
import com.vicente.taskmanager.service.FileStorageService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class FileStorageController implements FileStorageControllerDoc {
    private final FileStorageService fileStorageService;
    private static final Logger logger = LoggerFactory.getLogger(FileStorageController.class);

    public FileStorageController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(value = "/tasks/{taskId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileStorageResponseDTO> upload(
            @RequestParam("file") MultipartFile file,
            @PathVariable Long taskId,
            @AuthenticationPrincipal User user
    ){
        logger.debug("POST /api/v1/tasks/{taskId}/files upload called | userId={} taskId={} fileName={}",
                user.getId(), taskId, file.getOriginalFilename());

        FileStorageResponseDTO fileStorageResponseDTO = fileStorageService.upload(file, taskId, user.getId());

        URI uri = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/v1/files/{fileId}/download")
                .buildAndExpand(fileStorageResponseDTO.fileId()).toUri();

        return ResponseEntity.created(uri).body(fileStorageResponseDTO);
    }

    @PatchMapping("/files/{fileId}/rename")
    public ResponseEntity<FileStorageRenameResponseDTO> rename(
            @PathVariable Long fileId,
            @RequestBody @Valid FileStorageRenameRequestDTO fileStorageRenameRequestDTO,
            @AuthenticationPrincipal User user){
        logger.debug("PATH /api/v1/files/{fileId}/rename rename called | userId={} fileId={} newFileName={}",
                user.getId(), fileId, fileStorageRenameRequestDTO.newFileName());
        FileStorageRenameResponseDTO fileStorageRenameResponseDTO = fileStorageService.rename(
                fileId, user.getId(), fileStorageRenameRequestDTO.newFileName());
        return ResponseEntity.ok(fileStorageRenameResponseDTO);
    }

    @GetMapping("/tasks/{taskId}/files")
    public ResponseEntity<List<FileStorageResponseDTO>> findAllByTaskId(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User user
    ){
        logger.debug("GET /api/v1/tasks/{taskId}/files find all called | userId={} taskId={}", user.getId(), taskId);
        List<FileStorageResponseDTO> files = fileStorageService.findAllByTaskId(
                taskId, user.getId());
        return ResponseEntity.ok(files);
    }

    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<Resource> download(@PathVariable("fileId") Long id, @AuthenticationPrincipal User user){
        logger.debug("GET /api/v1/files/{fileId}/download download called | userId={} fileId={}", user.getId(), id);
        FileDownloadResult fileDownload = fileStorageService.download(id,  user.getId());

        InputStreamResource resource = new InputStreamResource(fileDownload.inputStream());

        // O UriUtils vai transformar espaços em %20, que é o mais seguro.
        String encoded = UriUtils.encode(fileDownload.filename(), StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileDownload.filename() + "\"; filename*=UTF-8''" +
                                encoded)
                .contentType(MediaType.parseMediaType(fileDownload.contentType()))
                .contentLength(fileDownload.size())
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        HttpHeaders.CONTENT_DISPOSITION)
                .body(resource);
    }

    @GetMapping("/files/{fileId}/share-url")
    public ResponseEntity<FileStorageDownloadUrlResponseDTO> shareUrl(
            @PathVariable("fileId") Long id,
            @AuthenticationPrincipal User user
    ){
        logger.debug("GET /api/v1/files/{fileId}/share-url shareUrl called | userId={} fileId={}", user.getId(), id);
        FileStorageDownloadUrlResponseDTO fileStorageDownloadUrlResponseDTO =
                fileStorageService.shareDownloadUrl(id,  user.getId());

        return ResponseEntity.ok(fileStorageDownloadUrlResponseDTO);
    }

    @DeleteMapping("/files/{fileId}/delete")
    public ResponseEntity<Void> delete(@PathVariable Long fileId, @AuthenticationPrincipal User user){
        logger.debug("DELETE /api/v1/files/{fileId}/delete delete called | userId={} fileId={}", user.getId(), fileId);
        fileStorageService.delete(fileId,  user.getId());
        return ResponseEntity.noContent().build();
    }
}
