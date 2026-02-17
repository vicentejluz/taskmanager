package com.vicente.taskmanager.controller;

import com.vicente.taskmanager.dto.request.UserUpdateRequestDTO;
import com.vicente.taskmanager.dto.response.PageResponseDTO;
import com.vicente.taskmanager.dto.response.UserResponseDTO;
import com.vicente.taskmanager.model.entity.User;
import com.vicente.taskmanager.service.UserService;
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

@Tag(name = "Users", description = "Users endpoints")
@RestController
@RequestMapping(value = "/api/v1/users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "Find user by ID",
            description = "Returns a user by its unique identifier"
    )
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> findById(@PathVariable Long id) {
        logger.debug("GET /api/v1/users/{id} findById called | userId={}", id);
        UserResponseDTO userResponseDTO = userService.findById(id);
        return ResponseEntity.ok(userResponseDTO);
    }

    @Operation(
            summary = "Find user by email",
            description = "Returns a user by its unique identifier"
    )
    @GetMapping(params = "email")
    public ResponseEntity<UserResponseDTO> findByEmail(@RequestParam String email) {
        logger.debug("GET /api/v1/users/{email} findByEmail called | email={}", email);
        UserResponseDTO userResponseDTO = userService.findByEmail(email);
        return ResponseEntity.ok(userResponseDTO);
    }

    @Operation(
            summary = "Find all users",
            description = "Returns paginated users."
    )
    @GetMapping("/findall")
    public ResponseEntity<PageResponseDTO<UserResponseDTO>> findAll(@ParameterObject Pageable pageable) {
        logger.debug("GET /api/v1/users findAll called");
        PageResponseDTO<UserResponseDTO> pageResponseDTO = userService.findAll(pageable);
        if (pageResponseDTO.content().isEmpty()) {
            logger.debug("GET /api/v1/users returned empty result");
        }
        return ResponseEntity.ok(pageResponseDTO);
    }

    @Operation(
            summary = "Update user",
            description = "Updates an existing user by ID"
    )
    @PatchMapping("/{id}")
    public ResponseEntity<UserResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequestDTO request,
            @AuthenticationPrincipal User user
    ) {
        logger.debug("PATCH /api/v1/users/{id} update called | id={}", id);
        UserResponseDTO responseDTO = userService.update(id, user.getId(), request);
        return ResponseEntity.ok(responseDTO);
    }
}
