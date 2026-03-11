package com.vicente.taskmanager.repository;

import com.vicente.taskmanager.domain.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findByUser_IdAndRevokedFalse(Long userId);
    List<RefreshToken> findByExpiresAtBefore(OffsetDateTime thresholdDate);
    List<RefreshToken> findByUser_DeletedAtBefore(OffsetDateTime thresholdDate);
}
