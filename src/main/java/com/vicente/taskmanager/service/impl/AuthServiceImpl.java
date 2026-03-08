package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.domain.entity.RefreshToken;
import com.vicente.taskmanager.dto.request.LoginRequestDTO;
import com.vicente.taskmanager.dto.request.PasswordRequestDTO;
import com.vicente.taskmanager.dto.request.RegisterUserRequestDTO;
import com.vicente.taskmanager.dto.response.TokenResponseDTO;
import com.vicente.taskmanager.dto.response.RegisterUserResponseDTO;
import com.vicente.taskmanager.exception.*;
import com.vicente.taskmanager.mapper.UserMapper;
import com.vicente.taskmanager.domain.entity.User;
import com.vicente.taskmanager.domain.entity.VerificationToken;
import com.vicente.taskmanager.domain.enums.AccountStatus;
import com.vicente.taskmanager.domain.enums.TokenType;
import com.vicente.taskmanager.domain.enums.UserRole;
import com.vicente.taskmanager.repository.UserRepository;
import com.vicente.taskmanager.security.service.TokenService;
import com.vicente.taskmanager.service.AuthService;
import com.vicente.taskmanager.service.EmailService;
import com.vicente.taskmanager.service.RefreshTokenService;
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
    private final RefreshTokenService refreshTokenService;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final AuthenticationManager authenticationManager;
    private final Long BASE_TIME_MINUTES;
    private final Integer MAX_ATTEMPTS;

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    public AuthServiceImpl(
            UserRepository userRepository, VerificationTokenService verificationTokenService,
            RefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder,
            TokenService tokenService, EmailService emailService,
            EntityManager entityManager,
            AuthenticationManager authenticationManager,
            @Value("${security.base.time.minutes}") Long BASE_TIME_MINUTES,
            @Value("${security.lock.max_attempts}") Integer MAX_ATTEMPTS
    ) {
        this.userRepository = userRepository;
        this.verificationTokenService = verificationTokenService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.entityManager = entityManager;
        this.authenticationManager = authenticationManager;
        this.BASE_TIME_MINUTES = BASE_TIME_MINUTES;
        this.MAX_ATTEMPTS = MAX_ATTEMPTS;
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
    @Transactional(noRollbackFor = {BadCredentialsException.class, AccountLockedException.class})
    public TokenResponseDTO login(LoginRequestDTO loginRequestDTO, String oldRefreshToken) {
        logger.info("Starting user login | email={}", loginRequestDTO.email());

        User user = userRepository.findByEmail(loginRequestDTO.email().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        try {
            Authentication authentication = getAuthentication(loginRequestDTO);

            user = (User) authentication.getPrincipal();
            String accessToken = tokenService.generateToken(Objects.requireNonNull(user));
            String refreshToken = refreshTokenService.create(Objects.requireNonNull(user), oldRefreshToken);

            user.resetFailedAttempts();

            logger.info("User logged in successfully. | userId={} email={}", user.getId(), user.getEmail());

            return new TokenResponseDTO(accessToken, refreshToken);
        }catch (BadCredentialsException e) {
            handleFailedLogin(Objects.requireNonNull(user));

            if(!user.isAccountNonLocked()) {
                logger.debug("User account locked | userId={}", user.getId());
                refreshTokenService.revokeAllTokens(user.getId());
                throw new AccountLockedException("User account is locked. Try again later.", user.getLockUntil());
            }

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
        logger.info("Validating token | tokenPrefix={}", token.substring(0,8));
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

        user.resetFailedAttempts();

        userRepository.saveAndFlush(user);

        verificationTokenService.consumeToken(verificationToken);

        refreshTokenService.revokeAllTokens(user.getId());

        emailService.sendPasswordResetSuccessEmail(user.getEmail(), ipAddress);
        logger.info("Password reset successfully | userId={} | email={}", user.getId(), user.getEmail());
    }

    @Override
    @Transactional
    public void logout(String token, Long userId) {
        refreshTokenService.revokeToken(token, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponseDTO refreshToken(String token) {
        logger.info("Starting refresh token process | refreshTokenPrefix={}",
                (token != null) ? token.substring(0,8) : null);
        RefreshToken refreshToken = refreshTokenService.validate(token);

        User user = userRepository.findById(refreshToken.getUser().getId()).orElseThrow(() -> {
                logger.debug("Invalid refresh token | userId={}", refreshToken.getUser().getId());
                return new UserNotFoundException("User not found");
                });

        String accessToken = tokenService.generateToken(user);
        logger.info("User refreshed token successfully | userId={}", user.getId());
        return new TokenResponseDTO(accessToken, refreshToken.getToken());
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
            user.registerFailedLoginAttempt(BASE_TIME_MINUTES, MAX_ATTEMPTS);
        }
    }

    private @NonNull Authentication getAuthentication(LoginRequestDTO loginRequestDTO) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginRequestDTO.email().toLowerCase().trim(),
                loginRequestDTO.password());
        return authenticationManager.authenticate(authenticationToken);
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

