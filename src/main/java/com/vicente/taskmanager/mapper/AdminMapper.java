package com.vicente.taskmanager.mapper;

import com.vicente.taskmanager.dto.response.PageResponseDTO;
import com.vicente.taskmanager.dto.response.UserAdminResponseDTO;
import com.vicente.taskmanager.model.entity.User;
import org.springframework.data.domain.Page;

public class AdminMapper {

    public static UserAdminResponseDTO toDTO(User user) {
        return  new UserAdminResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAccountStatus(),
                user.isEnabled(),
                user.isAccountNonLocked(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    public static PageResponseDTO<UserAdminResponseDTO> toPageDTO(Page<User> page) {
        Page<UserAdminResponseDTO> pageDTO = page.map(AdminMapper::toDTO);
        return new PageResponseDTO<>(
                pageDTO.getContent(),
                pageDTO.getNumber(),
                pageDTO.getSize(),
                pageDTO.getTotalPages(),
                pageDTO.getTotalElements(),
                pageDTO.isFirst(),
                pageDTO.isLast()
        );
    }

}
