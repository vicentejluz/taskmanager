package com.vicente.taskmanager.mapper;

import com.vicente.taskmanager.model.dto.request.RegisterUserRequestDTO;
import com.vicente.taskmanager.model.dto.response.RegisterUserResponseDTO;
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
}
