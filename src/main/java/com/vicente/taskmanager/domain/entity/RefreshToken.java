package com.vicente.taskmanager.domain.entity;

import com.vicente.taskmanager.domain.entity.base.BaseEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_refresh_tokens")
public class RefreshToken extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_family_id",  nullable = false)
    private UUID tokenFamilyId;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "reuse_detected", nullable = false)
    private boolean reuseDetected;

    @Column(nullable = false)
    private String fingerprint;

    public RefreshToken() {
    }

    public RefreshToken(String token, UUID tokenFamilyId, String fingerprint, OffsetDateTime expiresAt, User user) {
        this.token = token;
        this.tokenFamilyId = tokenFamilyId;
        this.fingerprint = fingerprint;
        this.expiresAt = expiresAt;
        this.user = user;
        this.reuseDetected = false;
    }

    public String getToken() {
        return token;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public User getUser() {
        return user;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public UUID getTokenFamilyId() {
        return tokenFamilyId;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setRevokedAt(OffsetDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public boolean isReuseDetected() {
        return reuseDetected;
    }
}
