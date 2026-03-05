package com.vicente.taskmanager.security.checker;

import com.vicente.taskmanager.exception.AccountDeletedException;
import com.vicente.taskmanager.exception.AccountLockedException;
import com.vicente.taskmanager.domain.entity.User;
import org.jspecify.annotations.NonNull;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class AccountStatusUserDetailsChecker implements UserDetailsChecker {

    protected final MessageSourceAccessor messages = SpringSecurityMessageSource
            .getAccessor();

    public void check(@NonNull UserDetails user) {
        if(user instanceof User domainUser && domainUser.getDeletedAt() != null){
            throw new AccountDeletedException(messages.getMessage("AccountStatusUserDetailsChecker.deleted",
                    "Invalid email or password"));
        }

        if (!user.isAccountNonLocked()) {
            if(user instanceof User domainUser) {
                OffsetDateTime lockedUntil = domainUser.getLockUntil();

                throw new AccountLockedException(messages.getMessage(
                        "AccountStatusUserDetailsChecker.locked",
                        "User account is locked"), lockedUntil);
            }

            throw new LockedException(messages.getMessage("AccountStatusUserDetailsChecker.locked",
                    "User account is locked"));
        }

        if (!user.isEnabled()) {
            throw new DisabledException(messages.getMessage(
                    "AccountStatusUserDetailsChecker.disabled", "User is disabled"));
        }

        if (!user.isAccountNonExpired()) {
            throw new AccountExpiredException(
                    messages.getMessage("AccountStatusUserDetailsChecker.expired",
                            "User account has expired"));
        }

        if (!user.isCredentialsNonExpired()) {
            throw new CredentialsExpiredException(messages.getMessage(
                    "AccountStatusUserDetailsChecker.credentialsExpired",
                    "User credentials have expired"));
        }
    }
}
