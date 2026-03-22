package com.vicente.taskmanager.controller.docs;

import com.vicente.taskmanager.domain.entity.User;
import com.vicente.taskmanager.dto.response.FileStorageResponseDTO;
import com.vicente.taskmanager.exception.error.StandardError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(
        name = "File Storage",
        description = "Endpoints for uploading, downloading, and deleting files associated with tasks"
)
@SecurityRequirement(name = "bearerAuth")
public interface FileStorageControllerDoc {

    @Operation(
            summary = "Upload a file",
            description = "Uploads a file and associates it with a specific task. " +
                    "Only allowed file extensions are accepted. The file is stored and metadata is persisted."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "File uploaded successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FileStorageResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input or file extension not allowed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "User does not have permission to access the task",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Task not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error during upload",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class))
            )
    })
    ResponseEntity<FileStorageResponseDTO> upload(
            @Parameter(
                    description = "File to be uploaded",
                    required = true
            )
            MultipartFile file,
            @Parameter(
                    description = "ID of the task to associate the file with",
                    example = "123",
                    required = true
            )
            Long taskId,
            User user
    );

    @Operation(
            summary = "List files by task",
            description = "Retrieves all files associated with a specific task. " +
                    "The authenticated user must be the owner of the task."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Files retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = FileStorageResponseDTO.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Task not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class))
            )
    })
    ResponseEntity<List<FileStorageResponseDTO>> findAllByTaskId(
            @Parameter(
                    description = "ID of the task to retrieve files from",
                    example = "123",
                    required = true
            )
            Long taskId,
            User user
    );

    @Operation(
            summary = "Download a file",
            description = "Downloads a file by its ID. The user must be the owner of the associated task."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "File downloaded successfully",
                    content = @Content(mediaType = "application/octet-stream")
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "User does not have permission to access the file",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FileStorageResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "File not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error during download",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class))
            )
    })
    ResponseEntity<Resource> download(
            @Parameter(
                    description = "Unique identifier of the file",
                    example = "123",
                    required = true
            )
            Long id,
            User user
    );

    @Operation(
            summary = "Delete a file",
            description = "Deletes a file by its ID. The user must be the owner of the associated task."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "File deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "User does not have permission to delete the file",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "File not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error during deletion",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class))
            )
    })
    ResponseEntity<Void> delete(
            @Parameter(
                    description = "Unique identifier of the file",
                    example = "123",
                    required = true
            )
            Long id,
            User user);
}
