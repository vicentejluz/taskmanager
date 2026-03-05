package com.vicente.taskmanager.service;

import com.vicente.taskmanager.domain.entity.User;
import com.vicente.taskmanager.domain.entity.VerificationToken;
import com.vicente.taskmanager.domain.enums.TokenType;

public interface VerificationTokenService {
    void consumeToken(VerificationToken verificationToken);
    void validateTokenForConsumption(VerificationToken verificationToken);
    VerificationToken generateOrReuseActiveToken(User user, TokenType tokenType);
    VerificationToken getOrCreateActiveVerificationToken(User user, TokenType tokenType);
    VerificationToken findByToken(String token);
}
