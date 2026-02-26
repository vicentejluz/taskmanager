package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.dto.request.LoginRequestDTO;
import com.vicente.taskmanager.dto.request.RegisterUserRequestDTO;
import com.vicente.taskmanager.dto.response.LoginResponseDTO;
import com.vicente.taskmanager.dto.response.RegisterUserResponseDTO;
import com.vicente.taskmanager.exception.*;
import com.vicente.taskmanager.mapper.UserMapper;
import com.vicente.taskmanager.model.entity.User;
import com.vicente.taskmanager.model.entity.VerificationToken;
import com.vicente.taskmanager.model.enums.AccountStatus;
import com.vicente.taskmanager.model.enums.UserRole;
import com.vicente.taskmanager.repository.UserRepository;
import com.vicente.taskmanager.security.service.TokenService;
import com.vicente.taskmanager.service.AuthService;
import com.vicente.taskmanager.service.EmailService;
import com.vicente.taskmanager.service.VerificationTokenService;
import jakarta.persistence.EntityManager;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final VerificationTokenService verificationTokenService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final EntityManager entityManager;
    private final AuthenticationManager authenticationManager;
    private final Long LOCK_MINUTES;
    private final Integer MAX_ATTEMPTS;

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    public AuthServiceImpl(
            UserRepository userRepository, VerificationTokenService verificationTokenService,
            PasswordEncoder passwordEncoder,
            TokenService tokenService, EmailService emailService,
            EntityManager entityManager,
            AuthenticationManager authenticationManager,
            @Value("${security.lock.minutes}") Long lockMinutes,
            @Value("${security.lock.max_attempts}") Integer maxAttempts
    ) {
        this.userRepository = userRepository;
        this.verificationTokenService = verificationTokenService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.entityManager = entityManager;
        this.authenticationManager = authenticationManager;
        LOCK_MINUTES = lockMinutes;
        MAX_ATTEMPTS = maxAttempts;
    }

    @Override
    @Transactional
    public RegisterUserResponseDTO register(RegisterUserRequestDTO registerUserRequest) {
        logger.info("Starting user registration | email={}", registerUserRequest.email());

        String email = registerUserRequest.email().toLowerCase().trim();

        Optional<User> optionalUser = userRepository.findByEmail(email);
        User user = prepareUserForRegistration(registerUserRequest, optionalUser);

        userRepository.saveAndFlush(user);
        entityManager.refresh(user);

        logger.info("User registered successfully | userId={}", user.getId());

        Optional<VerificationToken> existingToken =
                verificationTokenService.handleExistingActiveEmailVerificationToken(user);

        if (existingToken.isPresent()) {
            emailService.sendVerificationEmail(user.getEmail(), existingToken.get().getToken());
            return UserMapper.toDTO(user);
        }

        VerificationToken verificationToken = verificationTokenService.generateOrReuseActiveToken(user);

        emailService.sendVerificationEmail(email, verificationToken.getToken());

        return UserMapper.toDTO(user);
    }

    @Override
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {
        logger.info("Starting user login | email={}", loginRequestDTO.email());

        User user = findUserForLogin(loginRequestDTO);

        if(user.isLockExpired()){
            user.unlock();
        }

        try {
            Authentication authentication = getAuthentication(loginRequestDTO);

            user = (User) authentication.getPrincipal();
            String token = tokenService.generateToken(Objects.requireNonNull(user));

            user.resetFailedAttempts();

            logger.info("User logged in successfully. | userId={} email={}", user.getId(), user.getEmail());

            return new LoginResponseDTO(token);
        }catch (BadCredentialsException e) {
            handleFailedLogin(Objects.requireNonNull(user));
            throw e;
        }
    }

    private void handleFailedLogin(User user) {
        logger.debug("Bad credentials | email={}", user.getEmail());
        if(!user.getRoles().contains(UserRole.ADMIN)) {
            user.registerFailedLoginAttempt(LOCK_MINUTES, MAX_ATTEMPTS);
        }
    }

    private @NonNull Authentication getAuthentication(LoginRequestDTO loginRequestDTO) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginRequestDTO.email().toLowerCase().trim(),
                loginRequestDTO.password());
        return authenticationManager.authenticate(authenticationToken);
    }

    private @NonNull User findUserForLogin(LoginRequestDTO loginRequestDTO) {
        User user = userRepository.findByEmail(loginRequestDTO.email().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if(user.getDeletedAt() != null){
            logger.debug("User account has been deleted | email={}", user.getEmail());
            throw new AccountDeletedException("Invalid email or password");
        }
        return user;
    }

    private @NonNull User prepareUserForRegistration(RegisterUserRequestDTO registerUserRequest,
                                                     Optional<User> optionalUser) {
        User user;
        if(optionalUser.isPresent()) {
            user = optionalUser.get();
            validateExistingUserForRegistration(registerUserRequest, user);
            UserMapper.mergeExistEntity(user, registerUserRequest);

        }else{
            user = UserMapper.toEntity(registerUserRequest);
            user.getRoles().add(UserRole.USER);
        }

        user.setAccountStatus(AccountStatus.PENDING_VERIFICATION);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return user;
    }

    private void validateExistingUserForRegistration(RegisterUserRequestDTO registerUserRequest, User user) {
        if(user.getAccountStatus() != AccountStatus.PENDING_VERIFICATION && user.getDeletedAt() == null) {
            logger.debug("Registration attempt failed: email '{}' is already registered.",  registerUserRequest.email());
            throw new EmailAlreadyExistsException("Email already registered");
        }
    }
}

