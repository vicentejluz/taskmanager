package com.vicente.taskmanager.service;

import com.vicente.taskmanager.dto.filter.UserFilterDTO;
import com.vicente.taskmanager.dto.request.PasswordUpdateRequestDTO;
import com.vicente.taskmanager.dto.request.UserUpdateRequestDTO;
import com.vicente.taskmanager.dto.response.*;
import com.vicente.taskmanager.model.entity.User;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponseDTO getMe(User user);
    UserAdminResponseDTO findById(Long id);
    UserAdminResponseDTO findByEmail(String email);
    PageResponseDTO<UserAdminResponseDTO> find(UserFilterDTO filter, Pageable pageable);
    UserUpdateResponseDTO update(User authenticatedUser, UserUpdateRequestDTO userUpdateRequestDTO);
    void changePassword(User user, PasswordUpdateRequestDTO passwordUpdateRequestDTO);
    UserEnabledResponseDTO toggleUserEnabled(Long id);
    void delete(User user);
}
