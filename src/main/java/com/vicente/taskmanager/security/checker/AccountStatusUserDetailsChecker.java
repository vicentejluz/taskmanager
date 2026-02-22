package com.vicente.taskmanager.security.checker;

import com.vicente.taskmanager.exception.AccountLockedException;
import com.vicente.taskmanager.model.entity.User;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class AccountStatusUserDetailsChecker implements UserDetailsChecker {

    protected final MessageSourceAccessor messages = SpringSecurityMessageSource
            .getAccessor();

    public void check(UserDetails user) {
        if (!user.isAccountNonLocked()) {
            OffsetDateTime lockedUntil = ((User) user).getLockTime();

            throw new AccountLockedException(messages.getMessage(
                    "AccountStatusUserDetailsChecker.locked", "User account is locked"), lockedUntil);
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
