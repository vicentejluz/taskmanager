package com.vicente.taskmanager.service;

import com.vicente.taskmanager.dto.request.UserUpdateRequestDTO;
import com.vicente.taskmanager.dto.response.PageResponseDTO;
import com.vicente.taskmanager.dto.response.UserResponseDTO;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponseDTO findById(Long id);
    UserResponseDTO findByEmail(String email);
    PageResponseDTO<UserResponseDTO> findAll(Pageable pageable);
    UserResponseDTO update(Long id, Long userId, UserUpdateRequestDTO userUpdateRequestDTO);
}
