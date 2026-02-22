package com.vicente.taskmanager.service;

import com.vicente.taskmanager.dto.request.PasswordUpdateRequestDTO;
import com.vicente.taskmanager.dto.request.UserUpdateRequestDTO;
import com.vicente.taskmanager.dto.response.PageResponseDTO;
import com.vicente.taskmanager.dto.response.UserAdminResponseDTO;
import com.vicente.taskmanager.dto.response.UserResponseDTO;
import com.vicente.taskmanager.dto.response.UserUpdateResponseDTO;
import com.vicente.taskmanager.model.entity.User;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponseDTO getMe(User user);
    UserAdminResponseDTO findById(Long id);
    UserAdminResponseDTO findByEmail(String email);
    PageResponseDTO<UserAdminResponseDTO> find(String name, Boolean isEnabled, Boolean isAccountNonLocked, Pageable pageable);
    UserUpdateResponseDTO update(User authenticatedUser, UserUpdateRequestDTO userUpdateRequestDTO);
    void changePassword(User user, PasswordUpdateRequestDTO passwordUpdateRequestDTO);
    void updateUserEnabled(Long id, boolean enabled);
}
