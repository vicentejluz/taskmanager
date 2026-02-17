package com.vicente.taskmanager.mapper;

import com.vicente.taskmanager.dto.request.RegisterUserRequestDTO;
import com.vicente.taskmanager.dto.request.UserUpdateRequestDTO;
import com.vicente.taskmanager.dto.response.PageResponseDTO;
import com.vicente.taskmanager.dto.response.RegisterUserResponseDTO;
import com.vicente.taskmanager.dto.response.UserResponseDTO;
import com.vicente.taskmanager.model.entity.User;
import org.springframework.data.domain.Page;

public final class UserMapper {
    public static User toEntity(RegisterUserRequestDTO request) {
        return new User(request.name(), request.email(), request.password());
    }

    public static RegisterUserResponseDTO toDTO(User user) {
        return  new RegisterUserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public static UserResponseDTO toUserDTO(User user) {
        return  new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    public static PageResponseDTO<UserResponseDTO> toPageDTO(Page<UserResponseDTO> page) {
        return new PageResponseDTO<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isFirst(),
                page.isLast()
        );
    }

    public static void merge(User user, UserUpdateRequestDTO userUpdateRequestDTO) {
        if(userUpdateRequestDTO.name() != null && !userUpdateRequestDTO.name().isBlank())
            user.setName(userUpdateRequestDTO.name());
        if(userUpdateRequestDTO.email() != null && !userUpdateRequestDTO.email().isBlank())
            user.setEmail(userUpdateRequestDTO.email());
    }

}
