package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.dto.request.PasswordUpdateRequestDTO;
import com.vicente.taskmanager.dto.request.UserUpdateRequestDTO;
import com.vicente.taskmanager.dto.response.*;
import com.vicente.taskmanager.exception.*;

import com.vicente.taskmanager.mapper.AdminMapper;
import com.vicente.taskmanager.mapper.UserMapper;
import com.vicente.taskmanager.model.entity.User;
import com.vicente.taskmanager.model.enums.AccountStatus;
import com.vicente.taskmanager.model.enums.UserRole;
import com.vicente.taskmanager.repository.UserRepository;
import com.vicente.taskmanager.repository.specification.UserSpecification;
import com.vicente.taskmanager.dto.filter.UserFilterDTO;
import com.vicente.taskmanager.service.UserService;
import jakarta.persistence.EntityManager;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Objects;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, EntityManager entityManager, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.entityManager = entityManager;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    public UserResponseDTO getMe(User user) {
        logger.info("Starting getMe user | userId={}", user.getId());

        logger.info("Finished getMe user | userId={}", user.getId());
        return UserMapper.toUserDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserAdminResponseDTO findById(Long id) {
        logger.info("Starting findById user | id={}", id);
        User user = getUser(id);

        logger.info("Finished findById user | id={}", id);
        return AdminMapper.toDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserAdminResponseDTO findByEmail(String email) {
        logger.info("Starting findByEmail user | email={}", email);

        User user = userRepository.findByEmail(email.toLowerCase().trim()).orElseThrow(() -> {
            logger.debug("User not found with this email | email={}", email);
            return new UserNotFoundException("User Not found with this email " + email);
        });

        logger.info("Finished findByEmail user | email={}", email);
        return AdminMapper.toDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<UserAdminResponseDTO> find(UserFilterDTO filter, Pageable pageable) {
        logger.info("Starting find users with name {} and accountStatus {}",
                filter.name(), filter.accountStatus());

        logUserFindStrategy(filter);

        Specification<User> spec = UserSpecification.filter(filter);
        Page<User> users = userRepository.findAll(spec, pageable);

        logger.info("Find users success | totalElements={} totalPages={} page={} size={}", users.getTotalElements(),
                users.getTotalPages(), pageable.getPageNumber(), pageable.getPageSize());
        return AdminMapper.toPageDTO(users);
    }

    @Override
    @Transactional
    public UserUpdateResponseDTO update(User authenticatedUser, UserUpdateRequestDTO userUpdateRequestDTO) {
        logger.info("Starting update user | authenticatedUserId={}", authenticatedUser.getId());

        if(userRepository.existsByEmail(userUpdateRequestDTO.email().toLowerCase().trim()) &&
                !Objects.equals(authenticatedUser.getEmail(), userUpdateRequestDTO.email().toLowerCase().trim())){
            logger.debug("Registration attempt failed: email '{}' is already registered.",  authenticatedUser.getEmail());
            throw new EmailAlreadyExistsException("Email already registered");
        }

        UserMapper.merge(authenticatedUser, userUpdateRequestDTO);

        authenticatedUser = userRepository.saveAndFlush(authenticatedUser);
        entityManager.refresh(authenticatedUser);

        logger.info("User updated successfully | authenticatedUserId={}", authenticatedUser.getId());

        return UserMapper.toUserUpdateDTO(authenticatedUser);
    }

    @Override
    @Transactional
    public void changePassword(User authenticatedUser, PasswordUpdateRequestDTO passwordUpdateRequestDTO) {
        logger.info("Starting changePassword user | authenticatedUserId={}", authenticatedUser.getId());

        if(!passwordEncoder.matches(
                passwordUpdateRequestDTO.oldPassword(),
                authenticatedUser.getPassword())
        ) {
            logger.debug("Old password does not match user | authenticatedUser={}", authenticatedUser.getId());
            throw new InvalidOldPasswordException("Old password does not match.");
        }

        if(passwordEncoder.matches(
                passwordUpdateRequestDTO.newPassword(),
                authenticatedUser.getPassword())
        ) {
            throw new IllegalArgumentException(
                    "The new password must be different from the current password."
            );
        }

        authenticatedUser.setPassword(passwordEncoder.encode(passwordUpdateRequestDTO.newPassword()));

        logger.info("User password changed successfully. | authenticatedUserId={}", authenticatedUser.getId());

        userRepository.save(authenticatedUser);
    }

    @Override
    @Transactional
    public UserEnabledResponseDTO toggleUserEnabled(Long id) {
        logger.info("Starting toggleUserEnabled | id={}", id);
        User user = getUser(id);

        if(user.getDeletedAt() != null){
            logger.debug("Cannot toggle a deleted user");
            throw new InvalidUserStateTransitionException("Cannot toggle a deleted user");
        }

        if(user.getAccountStatus() != AccountStatus.ACTIVE && user.getAccountStatus() != AccountStatus.DISABLED_BY_ADMIN){
            logger.debug("Cannot toggle user with status | status={}", user.getAccountStatus());
            throw new InvalidUserStateTransitionException(
                    "Invalid state transition from status: " + user.getAccountStatus());
        }

        if(user.isEnabled()) {
            validateCanDisable(user);
            logger.info("User disabled by admin | id={}", id);
            user.setAccountStatus(AccountStatus.DISABLED_BY_ADMIN);
        }else {
            logger.info("User reactivated | id={}", id);
            user.setAccountStatus(AccountStatus.ACTIVE);
        }

        userRepository.save(user);

        logger.info("toggleUserEnabled finished | id={} enabled={}", id,  user.isEnabled());

        return UserMapper.toUserEnabledDTO(user);
    }

    @Override
    @Transactional
    public void delete(User user) {
        logger.info("Starting delete user | id={}", user.getId());

        validateCanDisable(user);

        user.setDeletedAt(OffsetDateTime.now());

        userRepository.save(user);

        logger.info("User deleted successfully | id={}", user.getId());
    }

    private void validateCanDisable(User user) {
        boolean isAdmin = user.getRoles().contains(UserRole.ADMIN);

        if (isAdmin) {
            throw new UserOperationNotAllowedException("This action is not allowed for administrator accounts.");
        }
    }


    private @NonNull User getUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> {
            logger.debug("User not found | userId={}", id);
            return new UserNotFoundException("User Not found");
        });
    }

    private void logUserFindStrategy(UserFilterDTO filter) {
        boolean hasName = filter.name() != null && !filter.name().isBlank();
        if(hasName && filter.accountStatus() != null) {
            logger.debug("Find strategy: name + accountStatus | name={} accountStatus={}", filter.name(), filter.accountStatus());
        } else if(hasName) {
            logger.debug("Find strategy: name | name={}", filter.name());
        } else if(filter.accountStatus() != null){
            logger.debug("Find strategy: accountStatus | accountStatus={}", filter.accountStatus());
        }else{
            logger.debug("Find strategy: all users");
        }
    }
}
