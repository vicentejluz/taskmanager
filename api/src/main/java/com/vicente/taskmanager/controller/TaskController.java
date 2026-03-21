package com.vicente.taskmanager.controller;

import com.vicente.taskmanager.controller.docs.TaskControllerDoc;
import com.vicente.taskmanager.dto.response.PageResponseDTO;
import com.vicente.taskmanager.dto.request.TaskCreateRequestDTO;
import com.vicente.taskmanager.dto.response.TaskResponseDTO;

import com.vicente.taskmanager.dto.request.TaskUpdateRequestDTO;
import com.vicente.taskmanager.domain.entity.User;
import com.vicente.taskmanager.service.TaskService;
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

@RestController
@RequestMapping(value = "/api/v1")
public class TaskController implements TaskControllerDoc {

    private final TaskService taskService;
    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    @GetMapping("/tasks/{id}")
    public ResponseEntity<TaskResponseDTO> findById(@PathVariable Long id, @AuthenticationPrincipal User user) {
        logger.debug("GET /api/v1/tasks/{id} findById called | taskId={} userId={}", id, user.getId());
        TaskResponseDTO responseDTO = taskService.findById(id, user.getId());
        return ResponseEntity.ok(responseDTO);
    }

    @Override
    @GetMapping("/tasks")
    public ResponseEntity<PageResponseDTO<TaskResponseDTO>> find(
            @RequestParam(required = false) String status,
            @RequestParam(value = "due-date", required = false) LocalDate dueDate,
            @AuthenticationPrincipal User user,
            @ParameterObject Pageable pageable
    ) {
        logger.debug("GET /api/v1/tasks find called | userId={} filters: status={} dueDate={}",
                user.getId(), status, dueDate);
        PageResponseDTO<TaskResponseDTO> pageResponseDTO = taskService.find(status, dueDate, user.getId(), pageable);
        if (pageResponseDTO.content().isEmpty()) {
            logger.debug("GET /api/v1/tasks returned empty result | userId={} filters: status={} dueDate={}",
                    user.getId(), status, dueDate);
        }
        return ResponseEntity.ok(pageResponseDTO);
    }

    @Override
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

    @Override
    @PatchMapping("/tasks/{id}")
    public ResponseEntity<TaskResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody TaskUpdateRequestDTO request,
            @AuthenticationPrincipal User user
    ) {
        logger.debug("PATCH /api/v1/tasks/{id} update called | taskId={} userId={}", id, user.getId());
        TaskResponseDTO responseDTO = taskService.update(id, user.getId(), request);
        return ResponseEntity.ok(responseDTO);
    }

    @Override
    @PatchMapping("/tasks/{id}/done")
    public ResponseEntity<TaskResponseDTO> done(@PathVariable Long id, @AuthenticationPrincipal User user) {
        logger.debug("PATCH /api/v1/tasks/done/{id} done called | taskId={} userId={}", id, user.getId());
        TaskResponseDTO responseDTO = taskService.done(id, user.getId());
        return ResponseEntity.ok(responseDTO);
    }

    @Override
    @PatchMapping("/tasks/{id}/cancel")
    public ResponseEntity<TaskResponseDTO> cancel(@PathVariable Long id, @AuthenticationPrincipal User user) {
        logger.debug("PATCH /api/v1/tasks/cancel/{id} cancelled called | taskId={} userId={}", id, user.getId());
        TaskResponseDTO responseDTO = taskService.cancel(id, user.getId());
        return ResponseEntity.ok(responseDTO);
    }

    @Override
    @DeleteMapping("/admin/tasks/{id}/delete")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id){
        logger.debug("DELETE /api/v1/admin/tasks/delete/{id} delete task called | id={}", id);
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/admin/users/{userId}/tasks")
    public ResponseEntity<PageResponseDTO<TaskResponseDTO>> adminFind(
            @RequestParam(required = false) String status,
            @RequestParam(value = "due-date", required = false) LocalDate dueDate,
            @PathVariable Long userId,
            @ParameterObject Pageable pageable
    ) {
        logger.debug("GET /api/v1/admin/users/{userId}/tasks admin find called | userId={} filters: status={} dueDate={}",
                userId, status, dueDate);
        PageResponseDTO<TaskResponseDTO> pageResponseDTO = taskService.find(status, dueDate, userId, pageable);
        if (pageResponseDTO.content().isEmpty()) {
            logger.debug("GET /api/v1/admin/users/{userId}/tasks returned empty result | userId={} filters: status={} dueDate={}",
                    userId, status, dueDate);
        }
        return ResponseEntity.ok(pageResponseDTO);
    }

    @Override
    @GetMapping("/admin/tasks/{id}")
    public ResponseEntity<TaskResponseDTO> getTask(@PathVariable Long id){
        logger.debug("GET /api/v1/admin/tasks/{id} getTask called | taskId={}", id);
        TaskResponseDTO taskResponseDTO = taskService.findById(id);
        return ResponseEntity.ok(taskResponseDTO);
    }
}
