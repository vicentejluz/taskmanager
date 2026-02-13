package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.dto.request.LoginRequestDTO;
import com.vicente.taskmanager.dto.request.RegisterUserRequestDTO;
import com.vicente.taskmanager.dto.response.RegisterUserResponseDTO;
import com.vicente.taskmanager.exception.EmailAlreadyExistsException;
import com.vicente.taskmanager.mapper.UserMapper;
import com.vicente.taskmanager.model.entity.User;
import com.vicente.taskmanager.model.entity.UserRole;
import com.vicente.taskmanager.repository.UserRepository;
import com.vicente.taskmanager.service.AuthService;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final AuthenticationManager authenticationManager;
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EntityManager entityManager,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
        this.authenticationManager = authenticationManager;
    }

    @Override
    @Transactional
    public RegisterUserResponseDTO register(RegisterUserRequestDTO registerUserRequest) {
        logger.info("Starting user registration | email={}", registerUserRequest.email());
        if(userRepository.existsByEmail(registerUserRequest.email())){
            logger.debug("Registration attempt failed: email '{}' is already registered.",  registerUserRequest.email());
            throw new EmailAlreadyExistsException("Email already registered");
        }

        User user = UserMapper.toEntity(registerUserRequest);

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.getRoles().add(UserRole.USER);

        userRepository.save(user);
        entityManager.refresh(user);

        logger.info("User registered successfully | userId={}", user.getId());

        return UserMapper.toDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public String login(LoginRequestDTO loginRequestDTO) {
        logger.info("Starting user login | email={}", loginRequestDTO.email());

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginRequestDTO.email(),
                loginRequestDTO.password());

        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        if (authentication.getPrincipal() != null) {
            User user = (User) authentication.getPrincipal();
            logger.info("User logged in successfully. | userId={} email={}", user.getId(), user.getEmail());
        }

        return "successfully logged in";
    }
}
