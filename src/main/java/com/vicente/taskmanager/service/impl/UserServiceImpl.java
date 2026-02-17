package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.dto.request.UserUpdateRequestDTO;
import com.vicente.taskmanager.dto.response.PageResponseDTO;
import com.vicente.taskmanager.dto.response.UserResponseDTO;
import com.vicente.taskmanager.exception.UserNotAllowedException;
import com.vicente.taskmanager.exception.UserNotFoundException;

import com.vicente.taskmanager.mapper.UserMapper;
import com.vicente.taskmanager.model.entity.User;
import com.vicente.taskmanager.repository.UserRepository;
import com.vicente.taskmanager.service.UserService;
import jakarta.persistence.EntityManager;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    public UserServiceImpl(UserRepository userRepository, EntityManager entityManager) {
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }


    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO findById(Long id) {
        logger.info("Starting find by id user | userId={}", id);
        User user = getUser(id);

        logger.info("Finished find by id user | userId={}", id);
        return UserMapper.toUserDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO findByEmail(String email) {
        logger.info("Starting find by email user | email={}", email);
        User user = userRepository.findByEmail(email).orElseThrow(() -> {
            logger.debug("User not found with this email | email={}", email);
            return new UserNotFoundException("User not found with this email");
        });

        logger.info("Finished find by email user | email={}", email);
        return UserMapper.toUserDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<UserResponseDTO> findAll(Pageable pageable) {
        logger.info("Starting find all users");
        Page<UserResponseDTO> users = userRepository.findAll(pageable).map(UserMapper::toUserDTO);

        logger.info("Finished find all users");
        return UserMapper.toPageDTO(users);
    }

    @Override
    @Transactional
    public UserResponseDTO update(Long id, Long userId, UserUpdateRequestDTO userUpdateRequestDTO) {
        logger.info("Starting update user | userId={}", id);

        if(!id.equals(userId)) {
            logger.debug("You do not have permission to access | id={} userId={}", id, userId);
            throw new UserNotAllowedException("User do not have permission to access it");
        }

        User user = getUser(id);

        UserMapper.merge(user, userUpdateRequestDTO);

        user = userRepository.saveAndFlush(user);
        entityManager.refresh(user);

        logger.info("User updated successfully | id={}", id);

        return UserMapper.toUserDTO(user);
    }

    private @NonNull User getUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> {
            logger.debug("User not found | userId={}", id);
            return new UserNotFoundException("User Not found");
        });
    }


}
