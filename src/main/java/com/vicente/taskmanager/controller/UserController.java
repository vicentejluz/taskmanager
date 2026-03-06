package com.vicente.taskmanager.controller;

import com.vicente.taskmanager.controller.docs.UserControllerDoc;
import com.vicente.taskmanager.dto.filter.UserFilterDTO;
import com.vicente.taskmanager.dto.request.PasswordUpdateRequestDTO;
import com.vicente.taskmanager.dto.request.UserUpdateRequestDTO;
import com.vicente.taskmanager.dto.response.*;
import com.vicente.taskmanager.domain.entity.User;
import com.vicente.taskmanager.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1")
public class UserController  implements UserControllerDoc {
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Override
    @GetMapping("/users/me")
    public ResponseEntity<UserResponseDTO> getMe(@AuthenticationPrincipal User user) {
        logger.debug("GET /api/v1/users/me getMe called | userId={}", user.getId());
        UserResponseDTO userResponseDTO = userService.getMe(user);
        return ResponseEntity.ok(userResponseDTO);
    }

    @Override
    @PatchMapping("/users/me")
    public ResponseEntity<UserUpdateResponseDTO> update(
            @Valid @RequestBody UserUpdateRequestDTO request,
            @AuthenticationPrincipal User user
    ) {
        logger.debug("PATCH /api/v1/users/update update called | authenticatedUserId={}", user.getId());
        UserUpdateResponseDTO responseDTO = userService.update(user, request);
        return ResponseEntity.ok(responseDTO);
    }

    @Override
    @PatchMapping("/users/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PasswordUpdateRequestDTO passwordUpdateRequestDTO
    ) {
        logger.debug("PATCH /api/v1/users/password changePassword called | authenticatedUserId={}", user.getId());
        userService.changePassword(user, passwordUpdateRequestDTO);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/users/me/delete")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user) {
        logger.debug("DELETE /api/v1/users/me/delete delete called | authenticatedUserId={}", user.getId());
        userService.delete(user);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/admin/users/{id}/enabled")
    public ResponseEntity<UserEnabledResponseDTO> toggleUserEnabled(@PathVariable Long id) {
        UserEnabledResponseDTO userEnabledResponseDTO = userService.toggleUserEnabled(id);
        return ResponseEntity.ok().body(userEnabledResponseDTO);
    }

    @Override
    @GetMapping("/admin/users/{id}")
    public ResponseEntity<UserAdminResponseDTO> findById(@PathVariable Long id) {
        logger.debug("GET /api/v1/users/{id} findById called | userId={}", id);
        UserAdminResponseDTO userResponseDTO = userService.findById(id);
        return ResponseEntity.ok(userResponseDTO);
    }

    @Override
    @GetMapping("/admin/users/by-email")
    public ResponseEntity<UserAdminResponseDTO> findByEmail(@RequestParam String email) {
        logger.debug("GET /api/v1/users/by-email findByEmail called | email={}", email);
        UserAdminResponseDTO userResponseDTO = userService.findByEmail(email);
        return ResponseEntity.ok(userResponseDTO);
    }

    @Override
    @GetMapping("/admin/users")
    public ResponseEntity<PageResponseDTO<UserAdminResponseDTO>> find(
            @ParameterObject UserFilterDTO filter,
            @ParameterObject Pageable pageable
    ) {
        logger.debug("GET /api/v1/admin/users find called | filters: name={} accountStatus={}",
                filter.name(), filter.accountStatus());
        PageResponseDTO<UserAdminResponseDTO> pageResponseDTO = userService.find(filter, pageable);
        if (pageResponseDTO.content().isEmpty()) {
            logger.debug("GET /api/v1/admin/users returned empty result");
        }
        return ResponseEntity.ok(pageResponseDTO);
    }
}
