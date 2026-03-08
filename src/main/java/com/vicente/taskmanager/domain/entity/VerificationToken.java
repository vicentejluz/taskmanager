package com.vicente.taskmanager.domain.entity;

import com.vicente.taskmanager.domain.entity.base.BaseEntity;
import com.vicente.taskmanager.domain.enums.TokenType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import java.time.OffsetDateTime;

@Entity
@Table(name = "tb_verification_tokens")
public class VerificationToken extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @JdbcType(value = PostgreSQLEnumJdbcType.class)
    @Column(name= "token_type", nullable = false)
    private TokenType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private boolean used;

    @Column(nullable = false)
    private boolean revoked;

    public VerificationToken() {
    }

    public VerificationToken(String token, TokenType type, OffsetDateTime expiresAt, User user) {
        this.token = token;
        this.type = type;
        this.expiresAt = expiresAt;
        this.user = user;
        this.used = false;
        this.revoked = false;
    }

    public String getToken() {
        return token;
    }

    public TokenType getType() {
        return type;
    }

    public User getUser() {
        return user;
    }

    public boolean isUsed() {
        return used;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setType(TokenType type) {
        this.type = type;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public boolean isExpired(){
        return this.expiresAt.isBefore(OffsetDateTime.now());
    }
}
