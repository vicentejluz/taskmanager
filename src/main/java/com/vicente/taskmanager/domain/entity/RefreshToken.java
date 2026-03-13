package com.vicente.taskmanager.domain.entity;

import com.vicente.taskmanager.domain.entity.base.BaseEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

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

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "reuse_detected", nullable = false)
    private boolean reuseDetected;

    public RefreshToken() {
    }

    public RefreshToken(String token, OffsetDateTime expiresAt, User user) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.user = user;
        this.revoked = false;
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

    public boolean isRevoked() {
        return revoked;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public boolean isReuseDetected() {
        return reuseDetected;
    }
}
