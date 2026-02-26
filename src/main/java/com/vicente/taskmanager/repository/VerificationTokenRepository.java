package com.vicente.taskmanager.repository;

import com.vicente.taskmanager.model.entity.VerificationToken;
import com.vicente.taskmanager.model.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByUser_IdAndTypeAndRevokedFalse(Long id, TokenType tokenType);
}
