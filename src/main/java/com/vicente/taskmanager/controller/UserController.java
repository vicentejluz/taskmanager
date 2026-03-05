package com.vicente.taskmanager.controller;

import com.vicente.taskmanager.controller.docs.UserControllerDoc;
import com.vicente.taskmanager.dto.filter.UserFilterDTO;
import com.vicente.taskmanager.dto.request.PasswordUpdateRequestDTO;
import com.vicente.taskmanager.dto.request.UserUpdateRequestDTO;
import com.vicente.taskmanager.dto.response.*;
import com.vicente.taskmanager.domain.entity.User;
import com.vicente.taskmanager.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
public class UserController  implements UserControllerDoc {
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Override
    public ResponseEntity<UserResponseDTO> getMe(User user) {
        logger.debug("GET /api/v1/users/me getMe called | userId={}", user.getId());
        UserResponseDTO userResponseDTO = userService.getMe(user);
        return ResponseEntity.ok(userResponseDTO);
    }

    @Override
    public ResponseEntity<UserUpdateResponseDTO> update(UserUpdateRequestDTO request, User user) {
        logger.debug("PATCH /api/v1/users/update update called | authenticatedUserId={}", user.getId());
        UserUpdateResponseDTO responseDTO = userService.update(user, request);
        return ResponseEntity.ok(responseDTO);
    }

    @Override
    public ResponseEntity<Void> changePassword(User user, PasswordUpdateRequestDTO passwordUpdateRequestDTO) {
        logger.debug("PATCH /api/v1/users/password changePassword called | authenticatedUserId={}", user.getId());
        userService.changePassword(user, passwordUpdateRequestDTO);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> delete(User user) {
        logger.debug("DELETE /api/v1/users/me/delete delete called | authenticatedUserId={}", user.getId());
        userService.delete(user);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<UserEnabledResponseDTO> toggleUserEnabled(Long id) {
        UserEnabledResponseDTO userEnabledResponseDTO = userService.toggleUserEnabled(id);
        return ResponseEntity.ok().body(userEnabledResponseDTO);
    }

    @Override
    public ResponseEntity<UserAdminResponseDTO> findById(Long id) {
        logger.debug("GET /api/v1/users/{id} findById called | userId={}", id);
        UserAdminResponseDTO userResponseDTO = userService.findById(id);
        return ResponseEntity.ok(userResponseDTO);
    }

    @Override
    public ResponseEntity<UserAdminResponseDTO> findByEmail(String email) {
        logger.debug("GET /api/v1/users/by-email findByEmail called | email={}", email);
        UserAdminResponseDTO userResponseDTO = userService.findByEmail(email);
        return ResponseEntity.ok(userResponseDTO);
    }

    @Override
    public ResponseEntity<PageResponseDTO<UserAdminResponseDTO>> find(UserFilterDTO filter, Pageable pageable) {
        logger.debug("GET /api/v1/admin/users find called | filters: name={} accountStatus={}",
                filter.name(), filter.accountStatus());
        PageResponseDTO<UserAdminResponseDTO> pageResponseDTO = userService.find(filter, pageable);
        if (pageResponseDTO.content().isEmpty()) {
            logger.debug("GET /api/v1/admin/users returned empty result");
        }
        return ResponseEntity.ok(pageResponseDTO);
    }
}
