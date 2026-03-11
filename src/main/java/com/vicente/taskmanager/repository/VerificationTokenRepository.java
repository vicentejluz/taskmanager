package com.vicente.taskmanager.repository;

import com.vicente.taskmanager.domain.entity.VerificationToken;
import com.vicente.taskmanager.domain.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByUser_IdAndType(Long id, TokenType tokenType);
    List<VerificationToken> findByTypeAndExpiresAtBefore(TokenType tokenType, OffsetDateTime thresholdDate);
}
