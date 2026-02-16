package com.vicente.taskmanager.controller;

import com.vicente.taskmanager.dto.response.PageResponseDTO;
import com.vicente.taskmanager.dto.request.TaskCreateRequestDTO;
import com.vicente.taskmanager.dto.response.TaskResponseDTO;

import com.vicente.taskmanager.dto.request.TaskUpdateRequestDTO;
import com.vicente.taskmanager.model.entity.User;
import com.vicente.taskmanager.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping(value = "/api/v1/tasks")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;
    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "Find task by ID", description = "Returns a task by its unique identifier")
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> findById(@PathVariable Long id, @AuthenticationPrincipal User user) {
        logger.debug("GET /api/v1/tasks/{id} findById called | taskId={} userId={}", id, user.getId());
        TaskResponseDTO responseDTO = taskService.findById(id, user.getId());
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(
            summary = "Find tasks",
            description = """
                    Returns paginated tasks.
                    Optional filters:
                    - status: Task status (PENDING, IN_PROGRESS, DONE, CANCELLED)
                    - due-date: Filter tasks by due date
                    """
    )
    @GetMapping
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

    @Operation(summary = "Create a new task", description = "Creates a new task and returns the created resource")
    @PostMapping
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

    @Operation(summary = "Update a task", description = "Updates an existing task by ID")
    @PatchMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> update(
            @PathVariable Long id, @Valid @RequestBody
            TaskUpdateRequestDTO request,
            @AuthenticationPrincipal User user
    ) {
        logger.debug("PATCH /api/v1/tasks/{id} update called | taskId={} userId={}", id, user.getId());
        TaskResponseDTO responseDTO = taskService.update(id, user.getId(), request);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Mark task as DONE", description = "Changes task status to DONE if allowed")
    @PatchMapping("/done/{id}")
    public ResponseEntity<TaskResponseDTO> done(@PathVariable Long id, @AuthenticationPrincipal User user) {
        logger.debug("PATCH /api/v1/tasks/done/{id} done called | taskId={} userId={}", id, user.getId());
        TaskResponseDTO responseDTO = taskService.done(id, user.getId());
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Cancel a task", description = "Changes task status to CANCELLED if allowed")
    @PatchMapping("/cancel/{id}")
    public ResponseEntity<TaskResponseDTO> cancel(@PathVariable Long id, @AuthenticationPrincipal User user) {
        logger.debug("PATCH /api/v1/tasks/cancel/{id} cancelled called | taskId={} userId={}", id, user.getId());
        TaskResponseDTO responseDTO = taskService.cancel(id, user.getId());
        return ResponseEntity.ok(responseDTO);
    }
}
