package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.dto.request.LoginRequestDTO;
import com.vicente.taskmanager.dto.request.RegisterUserRequestDTO;
import com.vicente.taskmanager.dto.response.LoginResponseDTO;
import com.vicente.taskmanager.dto.response.RegisterUserResponseDTO;
import com.vicente.taskmanager.exception.EmailAlreadyExistsException;
import com.vicente.taskmanager.mapper.UserMapper;
import com.vicente.taskmanager.model.entity.User;
import com.vicente.taskmanager.model.enums.UserRole;
import com.vicente.taskmanager.repository.UserRepository;
import com.vicente.taskmanager.security.service.TokenService;
import com.vicente.taskmanager.service.AuthService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EntityManager entityManager;
    private final AuthenticationManager authenticationManager;
    private final Long LOCK_MINUTES;
    private final Integer MAX_ATTEMPTS;

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            EntityManager entityManager,
            AuthenticationManager authenticationManager,
            @Value("${security.lock.minutes}") Long lockMinutes,
            @Value("${security.lock.max_attempts}") Integer maxAttempts
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.entityManager = entityManager;
        this.authenticationManager = authenticationManager;
        LOCK_MINUTES = lockMinutes;
        MAX_ATTEMPTS = maxAttempts;
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
    @Transactional(noRollbackFor = { BadCredentialsException.class, LockedException.class })
    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {
        logger.info("Starting user login | email={}", loginRequestDTO.email());

        User user = userRepository.findByEmail(loginRequestDTO.email()).orElseThrow(() ->
                new BadCredentialsException("Invalid email or password"));

        if(user.isLockExpired()){
            user.unlock();
        }

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginRequestDTO.email(),
                loginRequestDTO.password());

        try {
            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            user = (User) authentication.getPrincipal();
            String token = tokenService.generateToken(Objects.requireNonNull(user));

            user.resetFailedAttempts();

            logger.info("User logged in successfully. | userId={} email={}", user.getId(), user.getEmail());

            return new LoginResponseDTO(token);
        }catch (BadCredentialsException e) {
            logger.debug("Bad credentials | email={}", loginRequestDTO.email());
            Objects.requireNonNull(user).registerFailedLoginAttempt(LOCK_MINUTES, MAX_ATTEMPTS);
            throw e;
        }
    }
}

