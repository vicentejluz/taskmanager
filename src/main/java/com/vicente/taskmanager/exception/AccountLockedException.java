package com.vicente.taskmanager.exception;

import org.springframework.security.authentication.LockedException;

import java.time.OffsetDateTime;

public class AccountLockedException extends LockedException {

    private OffsetDateTime lockedUntil;

    public AccountLockedException(String message, OffsetDateTime lockedUntil) {
        super(message);
        this.lockedUntil = lockedUntil;
    }

    public AccountLockedException(String message) {
        super(message);
    }

    public OffsetDateTime getLockedUntil() {
        return lockedUntil;
    }
}
