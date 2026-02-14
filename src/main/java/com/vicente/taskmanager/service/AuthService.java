package com.vicente.taskmanager.service;

import com.vicente.taskmanager.dto.request.LoginRequestDTO;
import com.vicente.taskmanager.dto.request.RegisterUserRequestDTO;
import com.vicente.taskmanager.dto.response.LoginResponseDTO;
import com.vicente.taskmanager.dto.response.RegisterUserResponseDTO;

public interface AuthService {
    RegisterUserResponseDTO register(RegisterUserRequestDTO registerUserRequest);
    LoginResponseDTO login(LoginRequestDTO loginRequestDTO);
}
