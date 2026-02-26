package com.vicente.taskmanager.service;

import com.vicente.taskmanager.model.entity.User;
import com.vicente.taskmanager.model.entity.VerificationToken;

import java.util.Optional;

public interface VerificationTokenService {
    void consumeToken(String token);
    VerificationToken generateOrReuseActiveToken(User user);
    Optional<VerificationToken> handleExistingActiveEmailVerificationToken(User user);
}
