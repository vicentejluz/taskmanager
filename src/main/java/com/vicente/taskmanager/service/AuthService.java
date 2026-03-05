package com.vicente.taskmanager.service;

import com.vicente.taskmanager.dto.request.LoginRequestDTO;
import com.vicente.taskmanager.dto.request.PasswordRequestDTO;
import com.vicente.taskmanager.dto.request.RefreshTokenRequestDTO;
import com.vicente.taskmanager.dto.request.RegisterUserRequestDTO;
import com.vicente.taskmanager.dto.response.TokenResponseDTO;
import com.vicente.taskmanager.dto.response.RegisterUserResponseDTO;
import com.vicente.taskmanager.domain.enums.TokenType;

public interface AuthService {
    RegisterUserResponseDTO register(RegisterUserRequestDTO registerUserRequest);
    TokenResponseDTO login(LoginRequestDTO loginRequestDTO);
    void sendTokenEmail(String email, TokenType tokeType);
    void verifyEmail(String token);
    void validateToken(String token);
    void passwordReset(String token, PasswordRequestDTO passwordRequestDTO, String ipAddress);
    TokenResponseDTO refreshToken(RefreshTokenRequestDTO refreshTokenRequestDTO);
}
