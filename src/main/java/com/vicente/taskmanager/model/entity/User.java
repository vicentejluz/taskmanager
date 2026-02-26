package com.vicente.taskmanager.model.entity;

import com.vicente.taskmanager.model.enums.AccountStatus;
import com.vicente.taskmanager.model.enums.UserRole;
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
@Table(name = "tb_user")
public class User extends AbstractEntity implements UserDetails {
    @Column(nullable = false, length = 60)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false, length = 60)
    private String password;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_role", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(value =  EnumType.STRING)
    @JdbcType(value = PostgreSQLEnumJdbcType.class)
    @Column(name = "role", nullable = false)
    private Set<UserRole> roles = new HashSet<>();

    @Enumerated(value =  EnumType.STRING)
    @JdbcType(value = PostgreSQLEnumJdbcType.class)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus;

    @Column(name = "is_account_non_locked", nullable = false)
    private boolean isAccountNonLocked;

    @Column(name = "failed_attempts", nullable = false)
    private Integer failedAttempts;

    @Column(name = "lock_time")
    private OffsetDateTime lockTime;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public User() {
    }

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email == null ? null : email.toLowerCase().trim();
        this.password = password;
        this.isAccountNonLocked = true;
        this.failedAttempts = 0;
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

    @Override
    public boolean isAccountNonLocked() {
        return this.isAccountNonLocked;
    }

    public void setAccountNonLocked(boolean accountNonLocked) {
        isAccountNonLocked = accountNonLocked;
    }

    @Override
    @NonNull
    public String getUsername() {
        return email;
    }

    public Integer getFailedAttempts() {
        return failedAttempts;
    }

    public OffsetDateTime getLockTime() {
        return lockTime;
    }

    public void setLockTime(OffsetDateTime lockTime) {
        this.lockTime = lockTime;
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
    
    public void registerFailedLoginAttempt(long lockMinutes, int maxAttempts) {
        this.incrementFailedAttempts();
        if (this.failedAttempts >= maxAttempts) {
            this.isAccountNonLocked = false;
            this.lockTime = this.calculateLockExpiration(lockMinutes);
        }
    }

    public void resetFailedAttempts() {
        this.failedAttempts = 0;
    }

    public void unlock() {
        this.resetFailedAttempts();
        this.setLockTime(null);
        this.setAccountNonLocked(true);
    }

    public boolean isLockExpired() {
        return this.lockTime != null &&
                this.lockTime.isBefore(OffsetDateTime.now());
    }

    private void incrementFailedAttempts() {
        this.failedAttempts += 1;
    }

    private OffsetDateTime calculateLockExpiration(long lockMinutes) {
        return OffsetDateTime.now().plusMinutes(lockMinutes);
    }
}
