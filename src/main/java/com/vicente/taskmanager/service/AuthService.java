package com.vicente.taskmanager.service;

import com.vicente.taskmanager.dto.request.LoginRequestDTO;
import com.vicente.taskmanager.dto.request.PasswordRequestDTO;
import com.vicente.taskmanager.dto.request.RegisterUserRequestDTO;
import com.vicente.taskmanager.dto.response.TokenResponseDTO;
import com.vicente.taskmanager.dto.response.RegisterUserResponseDTO;
import com.vicente.taskmanager.domain.enums.TokenType;

public interface AuthService {
    RegisterUserResponseDTO register(RegisterUserRequestDTO registerUserRequest);
    TokenResponseDTO login(LoginRequestDTO loginRequestDTO, String oldRefreshToken);
    void sendTokenEmail(String email, TokenType tokeType);
    void verifyEmail(String token, String ipAddress);
    void validateToken(String token);
    void passwordReset(String token, PasswordRequestDTO passwordRequestDTO, String ipAddress);
    void logout(String refreshToken, String accessToken, Long userId);
    TokenResponseDTO refreshToken(String token);
}
