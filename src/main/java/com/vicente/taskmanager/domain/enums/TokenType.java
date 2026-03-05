package com.vicente.taskmanager.domain.enums;

public enum TokenType {
    EMAIL_VERIFICATION(60L),
    PASSWORD_RESET(20L);

    private final long expirationMinutes;

    TokenType(long expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }
}
