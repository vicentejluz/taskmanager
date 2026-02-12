package com.vicente.taskmanager.controller;

import com.vicente.taskmanager.exception.EmailAlreadyExistsException;
import com.vicente.taskmanager.mapper.UserMapper;
import com.vicente.taskmanager.model.dto.request.RegisterUserRequestDTO;
import com.vicente.taskmanager.model.dto.response.RegisterUserResponseDTO;
import com.vicente.taskmanager.model.entity.User;
import com.vicente.taskmanager.model.entity.UserRole;
import com.vicente.taskmanager.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Tag(name = "Authentication", description = "Authentication endpoints")
@RestController
@RequestMapping(value = "/api/v1/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    public AuthController(UserRepository userRepository, EntityManager entityManager) {
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<RegisterUserResponseDTO> registerUser(@Valid @RequestBody RegisterUserRequestDTO registerUserRequest)
    {
        if(userRepository.existsByEmail(registerUserRequest.email())){
            throw new EmailAlreadyExistsException("Email already registered");
        }
        User user = UserMapper.toEntity(registerUserRequest);
        user.getRole().add(UserRole.USER);

        userRepository.save(user);
        entityManager.refresh(user);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(user.getId()).toUri();

        return ResponseEntity.created(uri).body(UserMapper.toDTO(user));
    }
}
