package com.vicente.taskmanager.controller;

import com.vicente.taskmanager.model.dto.TaskCreateRequestDTO;
import com.vicente.taskmanager.model.dto.TaskResponseDTO;

import com.vicente.taskmanager.model.dto.TaskUpdateRequestDTO;
import com.vicente.taskmanager.service.TaskService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Tasks", description = "Task management endpoints")
@RestController
@RequestMapping(value = "/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<Page<TaskResponseDTO>> findAll(Pageable pageable) {
        Page<TaskResponseDTO> taskResponseDTOs = taskService.findAll(pageable);
        return ResponseEntity.ok(taskResponseDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> findById(@PathVariable Long id) {
        TaskResponseDTO responseDTO = taskService.findById(id);
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/findby")
    public ResponseEntity<List<TaskResponseDTO>> findByStatus(@RequestParam("status") String taskStatus) {
        List<TaskResponseDTO> responseDTO = taskService.findByStatus(taskStatus);
        if (responseDTO.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(responseDTO);
    }

    @PostMapping
    public ResponseEntity<TaskResponseDTO> create(@Valid @RequestBody TaskCreateRequestDTO request) {
        TaskResponseDTO responseDTO = taskService.create(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(responseDTO.id()).toUri();

        return ResponseEntity.created(uri).body(responseDTO);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> update(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequestDTO request) {
        TaskResponseDTO responseDTO = taskService.update(id, request);
        return ResponseEntity.ok(responseDTO);
    }

    @PatchMapping("/done/{id}")
    public ResponseEntity<TaskResponseDTO> done(@PathVariable Long id) {
        TaskResponseDTO responseDTO = taskService.done(id);
        return ResponseEntity.ok(responseDTO);
    }

    @PatchMapping("/cancel/{id}")
    public ResponseEntity<TaskResponseDTO> cancel(@PathVariable Long id) {
        TaskResponseDTO responseDTO = taskService.cancel(id);
        return ResponseEntity.ok(responseDTO);
    }
}
