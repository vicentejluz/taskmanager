package com.vicente.taskmanager.controller;

import com.vicente.taskmanager.model.domain.TaskStatus;
import com.vicente.taskmanager.model.dto.TaskRequestDTO;
import com.vicente.taskmanager.model.dto.TaskResponseDTO;

import com.vicente.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<TaskResponseDTO>> findAll() {
        List<TaskResponseDTO> taskResponseDTOS = taskService.findAll();
        return ResponseEntity.ok(taskResponseDTOS);
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
    public ResponseEntity<TaskResponseDTO> create(@Valid @RequestBody TaskRequestDTO request) {
        TaskResponseDTO responseDTO = taskService.create(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(responseDTO.id()).toUri();

        return ResponseEntity.created(uri).body(responseDTO);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> update(@PathVariable Long id, @Valid @RequestBody TaskRequestDTO request) {
        TaskResponseDTO responseDTO = taskService.update(id, request);
        return ResponseEntity.ok(responseDTO);
    }
}
