package com.vicente.taskmanager.domain.entity;

import com.vicente.taskmanager.domain.entity.base.BaseEntity;
import com.vicente.taskmanager.domain.enums.TokenType;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.generator.EventType;


import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_verification_tokens")
public class VerificationToken extends BaseEntity {
    @Column(
            nullable = false,
            unique = true,
            updatable = false,
            insertable = false,
            columnDefinition = "UUID DEFAULT uuidv7()"
    )
    @Generated(event = EventType.INSERT)
    @ColumnDefault("uuidv7()")
    private UUID token;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @JdbcType(value = PostgreSQLEnumJdbcType.class)
    @Column(name= "token_type", nullable = false)
    private TokenType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public VerificationToken() {
    }

    public VerificationToken(TokenType type, OffsetDateTime expiresAt, User user) {
        this.type = type;
        this.expiresAt = expiresAt;
        this.user = user;
    }

    public UUID getToken() {
        return token;
    }

    public TokenType getType() {
        return type;
    }

    public User getUser() {
        return user;
    }

    public void setType(TokenType type) {
        this.type = type;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isExpired(){
        return expiresAt != null && this.expiresAt.isBefore(OffsetDateTime.now());
    }
}
