package com.vicente.taskmanager.domain.entity;

import com.vicente.taskmanager.domain.entity.base.AuditedEntity;
import com.vicente.taskmanager.domain.enums.AccountStatus;
import com.vicente.taskmanager.domain.enums.UserRole;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "tb_users")
public class User extends AuditedEntity implements UserDetails {
    @Column(nullable = false, length = 60)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false, length = 60)
    private String password;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(value =  EnumType.STRING)
    @JdbcType(value = PostgreSQLEnumJdbcType.class)
    @Column(name = "role", nullable = false)
    private Set<UserRole> roles = new HashSet<>();

    @Enumerated(value =  EnumType.STRING)
    @JdbcType(value = PostgreSQLEnumJdbcType.class)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus;

    @Column(name = "failed_attempts", nullable = false)
    private Integer failedAttempts;

    @Column(name = "lock_until")
    private OffsetDateTime lockUntil;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "token_version", nullable = false)
    private long tokenVersion;

    public User() {
    }

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email == null ? null : email.toLowerCase().trim();
        this.password = password;
        this.failedAttempts = 0;
        this.tokenVersion = 0L;
    }

    @Override
    @NonNull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(role -> new SimpleGrantedAuthority(role.getValue()))
                .collect(Collectors.toUnmodifiableSet());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.toLowerCase().trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<UserRole> getRoles() {
        return roles;
    }

    public void setRoles(Set<UserRole> roles) {
        this.roles = roles;
    }

    public long getTokenVersion() {
        return tokenVersion;
    }

    public void incrementTokenVersion() {
        this.tokenVersion++;
    }

    @Override
    public boolean isEnabled() {
        return this.accountStatus ==  AccountStatus.ACTIVE
                && this.deletedAt == null;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public OffsetDateTime getLockUntil() {
        return lockUntil;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.lockUntil == null || this.lockUntil.isBefore(OffsetDateTime.now());
    }

    @Override
    @NonNull
    public String getUsername() {
        return email;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    public void registerFailedLoginAttempt(long baseTime, int maxAttempts) {
        this.incrementFailedAttempts();
        if (this.failedAttempts >= maxAttempts) {
            this.lockUntil = this.calculateLockExpiration(baseTime, maxAttempts);
        }
    }

    public void resetFailedAttempts() {
        this.lockUntil = null;
        this.failedAttempts = 0;
    }

    private void incrementFailedAttempts() {
        this.failedAttempts += 1;
    }

    private OffsetDateTime calculateLockExpiration(long baseTime, int maxAttempts) {
        final long MAX_LOCK_MINUTES = 1440;
        long blocks = failedAttempts - maxAttempts;
        long lockMinutes = (long) Math.min(baseTime * Math.pow(2, blocks), MAX_LOCK_MINUTES);
        return OffsetDateTime.now().plusMinutes(lockMinutes);
    }

    /*
     * Progressão exponencial do tempo de bloqueio usando deslocamento de bits (bit shift).
     *
     * A expressão:
     *
     *      (1L << blocks)
     *
     * significa "2 elevado a blocks" (2^blocks).
     *
     * O operador << é o operador de deslocamento de bits para a esquerda.
     * Ele move os bits de um número binário para a esquerda.
     *
     * Em binário:
     *
     *      1  = 0001
     *
     * Quando deslocamos 1 bit para a esquerda:
     *
     *      1L << 1  → 0010  = 2
     *
     * Dois deslocamentos:
     *
     *      1L << 2  → 0100  = 4
     *
     * Três deslocamentos:
     *
     *      1L << 3  → 1000  = 8
     *
     * Ou seja:
     *
     *      1L << n  =  2^n
     *
     * Portanto:
     *
     *      baseTime * (1L << blocks)
     *
     * é equivalente matematicamente a:
     *
     *      baseTime * 2^blocks
     *
     * Isso cria um crescimento exponencial do tempo de bloqueio (exponential backoff).
     *
     * Exemplo prático (baseTime = 30 minutos):
     *
     *      blocks = 0 → 30 * 2^0 = 30
     *      blocks = 1 → 30 * 2^1 = 60
     *      blocks = 2 → 30 * 2^2 = 120
     *      blocks = 3 → 30 * 2^3 = 240
     *      blocks = 4 → 30 * 2^4 = 480
     *      ...
     *
     * Esse comportamento é utilizado para:
     *  - Dificultar ataques de força bruta
     *  - Penalizar tentativas consecutivas
     *  - Aumentar progressivamente o tempo de bloqueio
     *
     * O valor final ainda é limitado (cap) por um tempo máximo permitido
     * para evitar bloqueios excessivamente longos.
     *
     * O uso de "1L" (long) ao invés de "1" (int) garante que o cálculo
     * seja feito utilizando 64 bits, reduzindo risco de overflow.
     */
    //long lockMinutes = baseTime * (1L << blocks);
}
