package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.dto.request.PasswordUpdateRequestDTO;
import com.vicente.taskmanager.dto.request.UserUpdateRequestDTO;
import com.vicente.taskmanager.dto.response.PageResponseDTO;
import com.vicente.taskmanager.dto.response.UserAdminResponseDTO;
import com.vicente.taskmanager.dto.response.UserResponseDTO;
import com.vicente.taskmanager.dto.response.UserUpdateResponseDTO;
import com.vicente.taskmanager.exception.EmailAlreadyExistsException;
import com.vicente.taskmanager.exception.InvalidOldPasswordException;
import com.vicente.taskmanager.exception.UserNotFoundException;

import com.vicente.taskmanager.exception.UserOperationNotAllowedException;
import com.vicente.taskmanager.mapper.UserMapper;
import com.vicente.taskmanager.model.entity.User;
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
        return UserMapper.toUserAdminDTO(user);
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
        return UserMapper.toUserAdminDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<UserAdminResponseDTO> find(UserFilterDTO filter, Pageable pageable) {
        logger.info("Starting find users with name {} and isEnabled {} and isAccountNonLocked {}",
                filter.name(), filter.enabled(), filter.accountNonLocked());

        logUserFindStrategy(filter);

        Specification<User> spec = UserSpecification.filter(filter);
        Page<UserAdminResponseDTO> users = userRepository.findAll(spec, pageable)
                .map(UserMapper::toUserAdminDTO);

        logger.info("Find users success | totalElements={} totalPages={} page={} size={}", users.getTotalElements(),
                users.getTotalPages(), pageable.getPageNumber(), pageable.getPageSize());
        return UserMapper.toPageDTO(users);
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
    public void updateUserEnabled(Long id, boolean enabled) {
        logger.info("Starting updateUserEnabled user | id={}", id);
        User user = getUser(id);

        if(!enabled) validateCanDisable(user);

        if(user.isEnabled() != enabled) {
            user.setEnabled(enabled);
            logger.info("User enabled successfully | id={} isEnable={}", id,  user.isEnabled());
            userRepository.save(user);
        }else{
            logger.debug("User already in desired state | id={} enabled={}", id, enabled);
        }
    }

    @Override
    @Transactional
    public void delete(User user) {
        logger.info("Starting delete user | id={}", user.getId());

        validateCanDisable(user);

        user.setEnabled(false);
        user.setDeletedAt();

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
        if(hasName && filter.enabled() != null && filter.accountNonLocked() != null) {
            logger.debug("Find strategy: name + enabled + accountNonLocked | name={} enabled={} accountNonLocked={}",
                    filter.name(), filter.enabled(), filter.accountNonLocked());
        } else if(hasName && filter.enabled() != null) {
            logger.debug("Find strategy: name + enabled | name={} enabled={}", filter.name(), filter.enabled());
        } else if(hasName && filter.accountNonLocked() != null) {
            logger.debug("Find strategy: name + accountNonLocked | name={} accountNonLocked={}",
                    filter.name(), filter.accountNonLocked());
        } else if(hasName) {
            logger.debug("Find strategy: name | name={}", filter.name());
        } else if(filter.enabled() != null && filter.accountNonLocked() != null) {
            logger.debug("Find strategy: enabled + accountNonLocked | enabled={} accountNonLocked={}",
                    filter.enabled(), filter.accountNonLocked());
        }else if(filter.enabled() != null){
            logger.debug("Find strategy: enabled | enabled={}", filter.enabled());
        }else if(filter.accountNonLocked() != null){
            logger.debug("Find strategy: accountNonLocked | enabled={}", filter.accountNonLocked());
        }else{
            logger.debug("Find strategy: all users");
        }
    }
}
