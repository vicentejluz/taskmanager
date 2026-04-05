package com.vicente.taskmanager.controller;

import com.vicente.taskmanager.controller.docs.LocalSignedFileControllerDoc;
import com.vicente.taskmanager.dto.internal.FileDownloadResult;
import com.vicente.taskmanager.service.LocalSignedFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/files")
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalSignedFileController implements LocalSignedFileControllerDoc {
    private final LocalSignedFileService localSignedFileService;
    private static final Logger logger = LoggerFactory.getLogger(LocalSignedFileController.class);

    public LocalSignedFileController(LocalSignedFileService localSignedFileService) {
        this.localSignedFileService = localSignedFileService;
    }

    @GetMapping("/taskmanager-files/{storageFilename}")
    public ResponseEntity<Resource> downloadSigned(
            @PathVariable("storageFilename") String storageFileName,
            @RequestParam("exp") Long expireAt,
            @RequestParam("token") String token,
            @RequestParam("rscd") String contentDisposition
    ){
        logger.debug("GET /api/v1/files/taskmanager-files/{storageFilename} downloadSigned called | " +
                        "storageFileName={} contentDisposition={}", storageFileName, contentDisposition);

        FileDownloadResult fileDownload = localSignedFileService.downloadSigned(
                token, storageFileName, expireAt, contentDisposition);

        InputStreamResource resource = new InputStreamResource(fileDownload.inputStream());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentType(MediaType.parseMediaType(fileDownload.contentType()))
                .contentLength(fileDownload.size())
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        HttpHeaders.CONTENT_DISPOSITION)
                .body(resource);
    }
}
