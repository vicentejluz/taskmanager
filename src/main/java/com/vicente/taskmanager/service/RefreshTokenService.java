package com.vicente.taskmanager.service;

import com.vicente.taskmanager.domain.entity.RefreshToken;
import com.vicente.taskmanager.domain.entity.User;

public interface RefreshTokenService {
    String create(User user);
    RefreshToken validate(String token);
    void revokeToken(String token, Long userId);
}
