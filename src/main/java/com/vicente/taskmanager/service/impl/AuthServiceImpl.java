package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.dto.request.LoginRequestDTO;
import com.vicente.taskmanager.dto.request.PasswordRequestDTO;
import com.vicente.taskmanager.dto.request.RegisterUserRequestDTO;
import com.vicente.taskmanager.dto.response.LoginResponseDTO;
import com.vicente.taskmanager.dto.response.RegisterUserResponseDTO;
import com.vicente.taskmanager.exception.*;
import com.vicente.taskmanager.mapper.UserMapper;
import com.vicente.taskmanager.model.entity.User;
import com.vicente.taskmanager.model.entity.VerificationToken;
import com.vicente.taskmanager.model.enums.AccountStatus;
import com.vicente.taskmanager.model.enums.TokenType;
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

        VerificationToken verificationToken = verificationTokenService.generateOrReuseActiveToken(
                user, TokenType.EMAIL_VERIFICATION);

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

    @Override
    @Transactional
    public void sendTokenEmail(String email, TokenType tokenType) {
        logger.info("Starting user resend verification | email={}", email);
        String normalizedEmail = email.toLowerCase().trim();

        User user = userRepository.findByEmail(normalizedEmail).orElseThrow(
                () -> new UserNotFoundException("Invalid email"));

        validateUserForTokenRequest(tokenType, user);

        VerificationToken verificationToken = verificationTokenService
                .getOrCreateActiveVerificationToken(user, tokenType);

        sendEmail(email, tokenType, user, verificationToken);

        logger.info("User resend successfully | email={}", email);
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        logger.info("Starting email verification process | token={}", token.substring(0,8));

        VerificationToken verificationToken = verificationTokenService.findByToken(token);
        if(verificationToken.getType() != TokenType.EMAIL_VERIFICATION) {
            logger.debug("Invalid token type for email verification | tokenId={}", verificationToken.getId());
            throw new VerificationTokenException("Invalid token type");
        }

        verificationTokenService.validateTokenForConsumption(verificationToken);
        User user = verificationToken.getUser();

        user.setAccountStatus(AccountStatus.ACTIVE);
        if (user.getDeletedAt() != null) user.setDeletedAt(null);

        userRepository.saveAndFlush(user);

        verificationTokenService.consumeToken(verificationToken);

        logger.info("Email verified successfully | userId={} | email={}", user.getId(), user.getEmail());
    }

    @Override
    public void validateToken(String token) {
        logger.info("Validating token | token={}", token.substring(0,8));
        VerificationToken verificationToken = verificationTokenService.findByToken(token);
        verificationTokenService.validateTokenForConsumption(verificationToken);
        logger.info("Token validated successfully | tokenId={}", verificationToken.getId());
    }

    @Override
    @Transactional
    public void passwordReset(String token, PasswordRequestDTO passwordRequestDTO, String ipAddress) {
        VerificationToken verificationToken = verificationTokenService.findByToken(token);

        if(verificationToken.getType() != TokenType.PASSWORD_RESET){
            logger.debug("Invalid token type for password reset | tokenId={}", verificationToken.getId());
            throw new VerificationTokenException("Invalid token type");
        }

        verificationTokenService.validateTokenForConsumption(verificationToken);

        User user = verificationToken.getUser();

        validateUserForTokenRequest(TokenType.PASSWORD_RESET, user);

        user.setPassword(passwordEncoder.encode(passwordRequestDTO.password()));

        userRepository.saveAndFlush(user);

        verificationTokenService.consumeToken(verificationToken);

        emailService.sendPasswordResetSuccessEmail(user.getEmail(), ipAddress);
        logger.info("Password reset successfully | userId={} | email={}", user.getId(), user.getEmail());
    }

    private void validateUserForTokenRequest(TokenType tokenType, User user) {
        if(tokenType == TokenType.EMAIL_VERIFICATION) {
            logger.info("Validating email verification token for email={}", user.getEmail());
            if (user.getAccountStatus() != AccountStatus.PENDING_VERIFICATION) {
                logger.debug("User is not pending verification | email={}", user.getEmail());
                throw new InvalidAccountStatusException("User is not pending verification");
            }
        }else{
            logger.info("Validating password reset token for email={}", user.getEmail());
            if(user.getDeletedAt() != null){
                logger.debug("User was deleted | email={}", user.getEmail());
                throw new AccountDeletedException("Invalid email");
            }
            if(user.getAccountStatus() != AccountStatus.ACTIVE){
                logger.debug("User is not active | email={}", user.getEmail());
                throw new InvalidAccountStatusException("User is not active");
            }
        }
    }

    private void sendEmail(String email, TokenType tokenType, User user, VerificationToken verificationToken) {
        if(tokenType == TokenType.EMAIL_VERIFICATION) {
            logger.debug("Sending verification email | email={}", user.getEmail());
            emailService.sendVerificationEmail(email, verificationToken.getToken());
        } else {
            logger.debug("Sending forgot password email | email={}", user.getEmail());
            emailService.sendForgotPasswordEmail(email, verificationToken.getToken());
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
            validateExistingUserForRegistration(user);
            UserMapper.mergeExistEntity(user, registerUserRequest);

        }else{
            user = UserMapper.toEntity(registerUserRequest);
            user.getRoles().add(UserRole.USER);
        }

        user.setAccountStatus(AccountStatus.PENDING_VERIFICATION);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return user;
    }

    private void validateExistingUserForRegistration(User user) {
        if (user.getDeletedAt() == null && user.getAccountStatus() == AccountStatus.ACTIVE) {
            logger.debug("User account is already active | email={}", user.getEmail());
            throw new EmailAlreadyExistsException("Email already registered.");
        }

        if (user.getAccountStatus() == AccountStatus.DISABLED_BY_ADMIN) {
            logger.debug("User has been disabled by admin | email={}", user.getEmail());
            throw new EmailAlreadyExistsException("Email already registered.");
        }

        if (user.getAccountStatus() == AccountStatus.PENDING_VERIFICATION) {
            logger.debug("User has been pending verification | email={}", user.getEmail());
            throw new EmailAlreadyExistsException(
                    "Email already registered but not verified. " +
                            "Please confirm your email or request a new verification link."
            );
        }
    }
}

