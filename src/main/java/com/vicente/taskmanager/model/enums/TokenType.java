package com.vicente.taskmanager.model.enums;

public enum TokenType {
    EMAIL_VERIFICATION(1L),
    PASSWORD_RESET(10L);

    private final long expirationMinutes;

    TokenType(long expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }
}
