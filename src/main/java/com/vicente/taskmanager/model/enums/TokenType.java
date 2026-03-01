package com.vicente.taskmanager.model.enums;

public enum TokenType {
    EMAIL_VERIFICATION(60L),
    PASSWORD_RESET(15L);

    private final long expirationMinutes;

    TokenType(long expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }
}
