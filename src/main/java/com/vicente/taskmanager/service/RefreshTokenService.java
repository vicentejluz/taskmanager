package com.vicente.taskmanager.service;

import com.vicente.taskmanager.domain.entity.RefreshToken;
import com.vicente.taskmanager.domain.entity.User;
import com.vicente.taskmanager.dto.internal.RefreshTokenResult;

public interface RefreshTokenService {
    RefreshTokenResult create(User user, String oldRefreshToken);
    String create(User user, RefreshToken oldRefreshToken, String oldFingerprint);
    RefreshToken findByTokenForUpdate(String token);
    boolean matchesFingerprint(RefreshToken oldRefreshToken, String fingerprint);
    void validateExpiration(RefreshToken refreshToken);
    void revokeToken(String token, Long userId);
    void revokeAllTokens(Long userId);
    void revokeAllTokensExceptCurrentToken(Long userId, String currentRefreshToken);
    void handleTokenCompromise(RefreshToken oldRefreshToken, String ipAddress);
}
