package com.vicente.taskmanager.mapper;

import com.vicente.taskmanager.dto.request.RegisterUserRequestDTO;
import com.vicente.taskmanager.dto.request.UserUpdateRequestDTO;
import com.vicente.taskmanager.dto.response.*;
import com.vicente.taskmanager.model.entity.User;

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
                user.getEmail());
    }

    public static UserUpdateResponseDTO toUserUpdateDTO(User user) {
        return  new UserUpdateResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    public static UserEnabledResponseDTO toUserEnabledDTO(User user) {
        return  new UserEnabledResponseDTO(
                user.getId(),
                user.getAccountStatus(),
                user.isEnabled()
        );
    }

    public static void merge(User user, UserUpdateRequestDTO userUpdateRequestDTO) {
        if(userUpdateRequestDTO.name() != null && !userUpdateRequestDTO.name().isBlank())
            user.setName(userUpdateRequestDTO.name());
        if(userUpdateRequestDTO.email() != null && !userUpdateRequestDTO.email().isBlank())
            user.setEmail(userUpdateRequestDTO.email());
    }

    public static void mergeExistEntity(User user, RegisterUserRequestDTO registerUserRequestDTO) {
        user.setName(registerUserRequestDTO.name());
        user.setPassword(registerUserRequestDTO.password());
    }

}
