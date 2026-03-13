package com.vicente.taskmanager.service;

import com.vicente.taskmanager.domain.entity.RefreshToken;
import com.vicente.taskmanager.domain.entity.User;

public interface RefreshTokenService {
    String create(User user, String oldRefreshToken);
    String create(User user, RefreshToken oldRefreshToken);
    RefreshToken findByTokenForUpdate(String token);
    void validate(RefreshToken refreshToken);
    void revokeToken(String token, Long userId);
    void revokeAllTokens(Long userId);
    void revokeAllTokensExceptCurrentToken(Long userId, String currentRefreshToken);
    void handleReuseAttack(RefreshToken oldRefreshToken, String ipAddress);
}
