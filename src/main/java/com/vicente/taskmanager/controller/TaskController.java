package com.vicente.taskmanager.controller;

import com.vicente.taskmanager.dto.response.PageResponseDTO;
import com.vicente.taskmanager.dto.request.TaskCreateRequestDTO;
import com.vicente.taskmanager.dto.response.TaskResponseDTO;

import com.vicente.taskmanager.dto.request.TaskUpdateRequestDTO;
import com.vicente.taskmanager.exception.error.StandardError;
import com.vicente.taskmanager.model.entity.User;
import com.vicente.taskmanager.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

@Tag(name = "Tasks", description = "Task management endpoints")
@RestController
@RequestMapping(value = "/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;
    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(
            summary = "Find task by ID",
            description = """
            Returns a task by its unique identifier.

            Rules:
            - The task must belong to the authenticated user.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Task found successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TaskResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Task not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Task does not belong to the authenticated user",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    @GetMapping("/tasks/{id}")
    public ResponseEntity<TaskResponseDTO> findById(@PathVariable Long id, @AuthenticationPrincipal User user) {
        logger.debug("GET /api/v1/tasks/{id} findById called | taskId={} userId={}", id, user.getId());
        TaskResponseDTO responseDTO = taskService.findById(id, user.getId());
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(
            summary = "Find tasks",
            description = """
            Returns paginated tasks for the authenticated user.

            Optional filters:
            - status: PENDING, IN_PROGRESS, DONE, CANCELLED
            - due-date: Filter by due date
            """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Tasks retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PageResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid filter parameters",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    @GetMapping("/tasks")
    public ResponseEntity<PageResponseDTO<TaskResponseDTO>> find(
            @RequestParam(required = false) String status,
            @RequestParam(value = "due-date", required = false) LocalDate dueDate,
            @AuthenticationPrincipal User user,
            @ParameterObject Pageable pageable) {
        logger.debug("GET /api/v1/tasks find called | userId={} filters: status={} dueDate={}",
                user.getId(), status, dueDate);
        PageResponseDTO<TaskResponseDTO> pageResponseDTO = taskService.find(status, dueDate, user.getId(), pageable);
        if (pageResponseDTO.content().isEmpty()) {
            logger.debug("GET /api/v1/tasks returned empty result | userId={} filters: status={} dueDate={}",
                    user.getId(), status, dueDate);
        }
        return ResponseEntity.ok(pageResponseDTO);
    }

    @Operation(
            summary = "Create a new task",
            description = """
            Creates a new task for the authenticated user.
            
            Validation rules:
            - Title must not be blank
            - Due date must not be in the past
            """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Task created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TaskResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    @PostMapping("/tasks")
    public ResponseEntity<TaskResponseDTO> create(
            @Valid @RequestBody TaskCreateRequestDTO request,
            @AuthenticationPrincipal User user
    ) {
        logger.debug("POST /api/v1/tasks create called");
        TaskResponseDTO responseDTO = taskService.create(request, user);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(responseDTO.id()).toUri();
        return ResponseEntity.created(uri).body(responseDTO);
    }

    @Operation(
            summary = "Update a task",
            description = """
            Updates an existing task by ID.

            Rules:
            - Task must belong to the authenticated user.
            - Status transition must be valid.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Task updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TaskResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error or invalid state transition",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Task not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    @PatchMapping("/tasks/{id}")
    public ResponseEntity<TaskResponseDTO> update(
            @PathVariable Long id, @Valid @RequestBody
            TaskUpdateRequestDTO request,
            @AuthenticationPrincipal User user
    ) {
        logger.debug("PATCH /api/v1/tasks/{id} update called | taskId={} userId={}", id, user.getId());
        TaskResponseDTO responseDTO = taskService.update(id, user.getId(), request);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(
            summary = "Mark task as DONE",
            description = """
            Changes the task status to DONE.

            Rules:
            - The task must belong to the authenticated user.
            - Only tasks in PENDING or IN_PROGRESS status can be marked as DONE.
            - CANCELLED tasks cannot be completed.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Task marked as DONE successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TaskResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid state transition",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Task does not belong to the authenticated user",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Task not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    @PatchMapping("/tasks/done/{id}")
    public ResponseEntity<TaskResponseDTO> done(@PathVariable Long id, @AuthenticationPrincipal User user) {
        logger.debug("PATCH /api/v1/tasks/done/{id} done called | taskId={} userId={}", id, user.getId());
        TaskResponseDTO responseDTO = taskService.done(id, user.getId());
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(
            summary = "Cancel a task",
            description = """
            Changes the task status to CANCELLED.

            Rules:
            - The task must belong to the authenticated user.
            - DONE tasks cannot be cancelled.
            - A task already CANCELLED cannot be cancelled again.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Task cancelled successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TaskResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid state transition",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Task does not belong to the authenticated user",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Task not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    @PatchMapping("/tasks/cancel/{id}")
    public ResponseEntity<TaskResponseDTO> cancel(@PathVariable Long id, @AuthenticationPrincipal User user) {
        logger.debug("PATCH /api/v1/tasks/cancel/{id} cancelled called | taskId={} userId={}", id, user.getId());
        TaskResponseDTO responseDTO = taskService.cancel(id, user.getId());
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(
            summary = "Admin - Delete task",
            description = """
            Deletes a task by ID.

            Requires ADMIN role.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Task deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Task not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Admin role required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    @DeleteMapping("/admin/tasks/delete/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id){
        logger.debug("DELETE /api/v1/admin/tasks/delete/{id} delete task called | id={}", id);
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Admin - Find user tasks",
            description = """
            Returns paginated tasks of a specific user.

            Requires ADMIN role.

            Optional filters:
            - status: PENDING, IN_PROGRESS, DONE, CANCELLED
            - due-date: Filter tasks by due date
            """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Tasks retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PageResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid filter parameters",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Admin role required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    @GetMapping("/admin/users/{userId}/tasks")
    public ResponseEntity<PageResponseDTO<TaskResponseDTO>> adminFind(
            @RequestParam(required = false) String status,
            @RequestParam(value = "due-date", required = false) LocalDate dueDate,
            @PathVariable Long userId,
            @ParameterObject Pageable pageable) {
        logger.debug("GET /api/v1/admin/users/{userId}/tasks admin find called | userId={} filters: status={} dueDate={}",
                userId, status, dueDate);
        PageResponseDTO<TaskResponseDTO> pageResponseDTO = taskService.find(status, dueDate, userId, pageable);
        if (pageResponseDTO.content().isEmpty()) {
            logger.debug("GET /api/v1/admin/users/{userId}/tasks returned empty result | userId={} filters: status={} dueDate={}",
                    userId, status, dueDate);
        }
        return ResponseEntity.ok(pageResponseDTO);
    }

    @Operation(
            summary = "Admin - Find task by ID",
            description = """
            Returns a task by its unique identifier.

            Requires ADMIN role.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Task found successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TaskResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Task not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Admin role required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    @GetMapping("/admin/tasks/{id}")
    public ResponseEntity<TaskResponseDTO> getTask(@PathVariable Long id){
        logger.debug("GET /api/v1/admin/tasks/{id} getTask called | taskId={}", id);
        TaskResponseDTO taskResponseDTO = taskService.findById(id);
        return ResponseEntity.ok(taskResponseDTO);
    }
}
