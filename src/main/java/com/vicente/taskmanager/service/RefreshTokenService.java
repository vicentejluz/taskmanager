package com.vicente.taskmanager.service;

import com.vicente.taskmanager.domain.entity.RefreshToken;
import com.vicente.taskmanager.domain.entity.User;

public interface RefreshTokenService {
    String create(User user, String oldRefreshToken);
    RefreshToken validate(String token);
    void revokeToken(String token, Long userId);
    void revokeAllTokens(Long userId);
    void revokeAllTokensExceptCurrentToken(Long userId, String currentRefreshToken);
}
