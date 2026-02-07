package com.vicente.taskmanager.controller;

import com.vicente.taskmanager.model.dto.PageResponseDTO;
import com.vicente.taskmanager.model.dto.TaskCreateRequestDTO;
import com.vicente.taskmanager.model.dto.TaskResponseDTO;

import com.vicente.taskmanager.model.dto.TaskUpdateRequestDTO;
import com.vicente.taskmanager.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;


@Tag(name = "Tasks", description = "Task management endpoints")
@RestController
@RequestMapping(value = "/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "Find task by ID", description = "Returns a task by its unique identifier")
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> findById(@PathVariable Long id) {
        TaskResponseDTO responseDTO = taskService.findById(id);
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
            @RequestParam(required = false)  String status,
            @RequestParam(value = "due-date", required = false) LocalDate dueDate,
            Pageable pageable) {
        PageResponseDTO<TaskResponseDTO> pageResponseDTO = taskService.find(status, dueDate, pageable);
        if (pageResponseDTO.content().isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(pageResponseDTO);
    }

    @Operation(summary = "Create a new task", description = "Creates a new task and returns the created resource")
    @PostMapping
    public ResponseEntity<TaskResponseDTO> create(@Valid @RequestBody TaskCreateRequestDTO request) {
        TaskResponseDTO responseDTO = taskService.create(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(responseDTO.id()).toUri();

        return ResponseEntity.created(uri).body(responseDTO);
    }

    @Operation(summary = "Update a task", description = "Updates an existing task by ID")
    @PatchMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> update(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequestDTO request) {
        TaskResponseDTO responseDTO = taskService.update(id, request);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Mark task as DONE", description = "Changes task status to DONE if allowed")
    @PatchMapping("/done/{id}")
    public ResponseEntity<TaskResponseDTO> done(@PathVariable Long id) {
        TaskResponseDTO responseDTO = taskService.done(id);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Cancel a task", description = "Changes task status to CANCELLED if allowed")
    @PatchMapping("/cancel/{id}")
    public ResponseEntity<TaskResponseDTO> cancel(@PathVariable Long id) {
        TaskResponseDTO responseDTO = taskService.cancel(id);
        return ResponseEntity.ok(responseDTO);
    }
}
